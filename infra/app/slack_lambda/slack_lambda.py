import base64
import os
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

profile = os.environ.get("PROFILE")
slack_channel = os.environ.get("SLACK_CHANNEL")

region = "eu-west-2"
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

    messages = [event["message"] for event in log_event["logEvents"]]
    log_event["message"] = "\n".join(messages)

    return log_event


def logStreamUrl(region, log_group, log_stream):
    return (
        "https://console.aws.amazon.com"
        + "/cloudwatch/home?"
        + f"region={region}"
        + "#logEventViewer:"
        + f"group={log_group};"
        + f"stream={log_stream}"
    )


def ecsTaskUrl(region, cluster, task_id):
    return (
        f"https://{region}.console.aws.amazon.com"
        + "/ecs/v2"
        + f"/clusters/{cluster}"
        + f"/tasks/{task_id}"
    )


def send_slack_message(channel, text):
    body = {
        "channel": channel,
        "text": text,
    }

    logger.debug("Sending message to slack")

    http.request(
        "POST", base_url + "/chat.postMessage", headers=headers, body=json.dumps(body)
    )


# >> Handler

warning_filters = [
    re.compile(r"(?i)WARN"),
]

error_filters = [
    re.compile(r"(?i)ERROR"),
    re.compile(r"(?i)FATAL"),
    re.compile(r"(?i)CRITICAL"),
]

log_filters = warning_filters + error_filters


def handle_awslogs(event, context):
    if "awslogs" not in event:
        logger.info("No awslogs in event")
        return

    # >> Parse Event
    log_event = parseLogEvent(event["awslogs"])
    url = logStreamUrl(region, log_event["logGroup"], log_event["logStream"])
    # TODO: Truncate?
    message = log_event["message"]

    logger.debug("Message: %s", message)
    logger.debug("Url: %s", url)
    logger.debug("Log Event: %s", json.dumps(log_event))

    # >> Test if message matches any filters

    found_match = False

    for filter in log_filters:
        if re.search(filter, message):
            found_match = True
            break

    if not found_match:
        logger.debug("Message matched no filter")
        return

    # >> Send message on to slack
    link_text = "Logs " + log_event["logGroup"]
    text = message + f"\n<{url}|{link_text}>"
    send_slack_message(slack_channel, text)


def handle_ecs_task_state_change(event, context):
    detail = event["detail"]

    # >> Collect info
    cluster_name = detail["clusterArn"].split("/")[-1]
    task_id = detail["taskArn"].split("/")[-1]
    last_status = detail["lastStatus"]

    logger.debug("Cluster: %s", cluster_name)
    logger.debug("Task ID: %s", task_id)
    logger.debug("Last Status: %s", last_status)

    container_statuses = []
    for container in detail["containers"]:
        container_name = container["name"]
        container_status = container["lastStatus"]
        logger.debug("Container: %s", container_name)
        logger.debug("Container Status: %s", container_status)

        container_statuses.append(f"{container_name}: `{container_status}`")

    # >> Write message
    task_url = ecsTaskUrl(region, cluster_name, task_id)

    truncated_task_id = task_id[:8]
    message = (
        f"<{task_url}|{cluster_name}/{truncated_task_id}> -> `{last_status}`"
        + "\nContainers:"
        + "\n"
        + "\n".join(container_statuses)
    )

    send_slack_message(slack_channel, message)


def handler(event, context):
    if "awslogs" in event:
        handle_awslogs(event, context)
    elif "detail-type" in event and event["detail-type"] == "ECS Task State Change":
        handle_ecs_task_state_change(event, context)
    else:
        logger.info("Event not recognised")
        logger.debug("Event: %s", json.dumps(event))


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
        "awslogs": {
            "data": base64.b64decode(json.dumps(data)),
        },
    }
    handler(event, None)
