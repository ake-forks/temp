import base64
import boto3
import gzip
import json
import logging
import re
import urllib3


# >> Setup

http = urllib3.PoolManager()
logging.basicConfig()
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
ssm = boto3.client("ssm")


# >> Config

response = ssm.get_parameter(Name="ProbateTree_SlackToken", WithDecryption=True)
slack_token = response["Parameter"]["Value"]

base_url = "https://slack.com/api"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer " + slack_token,
}


# >> Utils


def parseLogEvent(awsLogs):
    logger.debug(awsLogs["data"])

    compressed_payload = base64.b64decode(awsLogs["data"])
    uncompressed_payload = gzip.decompress(compressed_payload)
    log_event = json.loads(uncompressed_payload)

    full_msg = ""
    for event in log_event["logEvents"]:
        full_msg += event["message"]

    log_event["message"] = full_msg

    return log_event


def logStreamUrl(region, log_group, log_stream):
    return (
        "https://console.aws.amazon.com"
        + "/cloudwatch/home?"
        + "region={region}"
        + "#logEventViewer:"
        + "group={log_group};"
        + "stream={log_stream}"
    ).format(region=region, log_group=log_group, log_stream=log_stream)


# >> Handler
warning_filters = [
    re.compile(r"(?i)WARN"),
]

error_filters = [
    re.compile(r"(?i)ERROR"),
    re.compile(r"(?i)FATAL"),
    re.compile(r"(?i)CRITICAL"),
]

filters = warning_filters + error_filters


def handler(event, context):

    if "awsLogs" not in event:
        logger.info("No awsLogs in event")
        return

    # >> Parse Event
    log_event = parseLogEvent(event["awslogs"])
    url = logStreamUrl("eu-west-2", log_event["logGroup"], log_event["logStream"])
    # TODO: Truncate?
    message = log_event["message"]

    logger.debug("Message: %s", message)
    logger.debug("Url: %s", url)
    logger.debug("Log Event: %s", json.dumps(log_event))

    # >> Test if message matches any filters

    found_match = False

    for filter in filters:
        if re.search(filter, message):
            found_match = True
            break

    if not found_match:
        logger.debug("Message matched no filters")
        return

    # >> Send message on to slack
    logger.debug("Sending message to slack")

    link_text = "Logs " + log_event["logGroup"]
    body = {
        "channel": "darbylaw-ops",
        "text": message + "\n<" + url + "|" + link_text + ">",
    }

    http.request(
        "POST", base_url + "/chat.postMessage", headers=headers, body=json.dumps(body)
    )


if __name__ == "__main__":
    data = {
        "messageType": "DATA_MESSAGE",
        "owner": "123456789012",
        "logGroup": "/test/log_group",
        "logStream": "my-test-stream",
        "subscriptionFilters": [
            "test_to_slack",
        ],
        "logEvents": [
            {
                "id": "123456789",
                "timestamp": 123456789,
                "message": "ERROR: This is a test message",
            }
        ],
    }
    event = {
        "awsLogs": {
            "data": base64.b64decode(json.dumps(data)),
        },
    }
    handler(event, None)
