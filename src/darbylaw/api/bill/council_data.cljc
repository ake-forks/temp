(ns darbylaw.api.bill.council-data)
(def councils
  [{:id :adur-and-worthing-borough,
    :common-name "Adur and Worthing Borough Council"}
   {:id :adur-district, :common-name "Adur District Council"}
   {:id :allerdale-borough, :common-name "Allerdale Borough Council"}
   {:id :amber-valley-borough, :common-name "Amber Valley Borough Council"}
   {:id :arun-district, :common-name "Arun District Council"}
   {:id :ashfield-district, :common-name "Ashfield District Council"}
   {:id :ashford-borough, :common-name "Ashford Borough Council"}
   {:id :aylesbury-vale-district,
    :common-name "Aylesbury Vale District Council"}
   {:id :babergh-district, :common-name "Babergh District Council"}
   {:id :barnsley-metropolitan-borough,
    :common-name "Barnsley Metropolitan Borough Council"}
   {:id :barrowinfurness-borough,
    :common-name "Barrow-in-Furness Borough Council"}
   {:id :basildon-borough, :common-name "Basildon Borough Council"}
   {:id :basingstoke-and-deane-borough,
    :common-name "Basingstoke and Deane Borough Council"}
   {:id :bassetlaw-district, :common-name "Bassetlaw District Council"}
   {:id :bath-and-north-east-somerset,
    :common-name "Bath and North East Somerset Council"}
   {:id :bedford-borough, :common-name "Bedford Borough Council"}
   {:id :birmingham-city, :common-name "Birmingham City Council"}
   {:id :blaby-district, :common-name "Blaby District Council"}
   {:id :blackburn-with-darwen-borough,
    :common-name "Blackburn with Darwen Borough Council"}
   {:id :blackpool-borough, :common-name "Blackpool Borough Council"}
   {:id :blaenau-gwent-county-borough,
    :common-name "Blaenau Gwent County Borough Council"}
   {:id :bolsover-district, :common-name "Bolsover District Council"}
   {:id :bolton-metropolitan-borough,
    :common-name "Bolton Metropolitan Borough Council"}
   {:id :borough-of-broxbourne, :common-name "Borough of Broxbourne"}
   {:id :borough-of-poole, :common-name "Borough of Poole"}
   {:id :boston-borough, :common-name "Boston Borough Council"}
   {:id :bournemouth-borough, :common-name "Bournemouth Borough Council"}
   {:id :bracknell-forest, :common-name "Bracknell Forest Council"}
   {:id :bradford-metropolitan-district,
    :common-name "Bradford Metropolitan District Council"}
   {:id :braintree-district, :common-name "Braintree District Council"}
   {:id :breckland-district, :common-name "Breckland District Council"}
   {:id :brentwood-borough, :common-name "Brentwood Borough Council"}
   {:id :bridgend-county-borough,
    :common-name "Bridgend County Borough Council"}
   {:id :brighton-and-hove-city,
    :common-name "Brighton and Hove City Council"}
   {:id :bristol-city, :common-name "Bristol City Council"}
   {:id :broadland-district, :common-name "Broadland District Council"}
   {:id :bromsgrove-district, :common-name "Bromsgrove District Council"}
   {:id :broxtowe-borough, :common-name "Broxtowe Borough Council"}
   {:id :buckinghamshire-county,
    :common-name "Buckinghamshire County Council"}
   {:id :burnley-borough, :common-name "Burnley Borough Council"}
   {:id :bury-metropolitan-borough,
    :common-name "Bury Metropolitan Borough Council"}
   {:id :caerphilly-county-borough,
    :common-name "Caerphilly County Borough Council"}
   {:id :calderdale-metropolitan-borough,
    :common-name "Calderdale Metropolitan Borough Council"}
   {:id :cambridge-city, :common-name "Cambridge City Council"}
   {:id :cambridgeshire-county,
    :common-name "Cambridgeshire County Council"}
   {:id :cannock-chase-district,
    :common-name "Cannock Chase District Council"}
   {:id :canterbury-city, :common-name "Canterbury City Council"}
   {:id :cardiff, :common-name "Cardiff Council"}
   {:id :carlisle-city, :common-name "Carlisle City Council"}
   {:id :carmarthenshire-county,
    :common-name "Carmarthenshire County Council"}
   {:id :castle-point-borough, :common-name "Castle Point Borough Council"}
   {:id :central-bedfordshire, :common-name "Central Bedfordshire Council"}
   {:id :ceredigion-county, :common-name "Ceredigion County Council"}
   {:id :charnwood-borough, :common-name "Charnwood Borough Council"}
   {:id :chelmsford-city, :common-name "Chelmsford City Council"}
   {:id :cheltenham-borough, :common-name "Cheltenham Borough Council"}
   {:id :cherwell-district, :common-name "Cherwell District Council"}
   {:id :cheshire-east-unitary,
    :common-name "Cheshire East Council (Unitary)"}
   {:id :cheshire-west-and-chester,
    :common-name "Cheshire West and Chester Council"}
   {:id :chesterfield-borough, :common-name "Chesterfield Borough Council"}
   {:id :chichester-district, :common-name "Chichester District Council"}
   {:id :chiltern-district, :common-name "Chiltern District Council"}
   {:id :chorley, :common-name "Chorley Council"}
   {:id :christchurch-borough, :common-name "Christchurch Borough Council"}
   {:id :city-of-lincoln, :common-name "City of Lincoln Council"}
   {:id :city-of-london, :common-name "City of London"}
   {:id :city-of-york, :common-name "City of York Council"}
   {:id :colchester-borough, :common-name "Colchester Borough Council"}
   {:id :conwy-county-borough, :common-name "Conwy County Borough Council"}
   {:id :copeland-borough, :common-name "Copeland Borough Council"}
   {:id :corby-borough, :common-name "Corby Borough Council"}
   {:id :cornwall-unitary, :common-name "Cornwall Council (Unitary)"}
   {:id :cotswold-district, :common-name "Cotswold District Council"}
   {:id :coventry-city, :common-name "Coventry City Council"}
   {:id :craven-district, :common-name "Craven District Council"}
   {:id :crawley-borough, :common-name "Crawley Borough Council"}
   {:id :cumbria-county, :common-name "Cumbria County Council"}
   {:id :dacorum, :common-name "Dacorum Council"}
   {:id :darlington-borough, :common-name "Darlington Borough Council"}
   {:id :dartford-borough, :common-name "Dartford Borough Council"}
   {:id :daventry-district, :common-name "Daventry District Council"}
   {:id :denbighshire-county, :common-name "Denbighshire County Council"}
   {:id :derby-city, :common-name "Derby City Council"}
   {:id :derbyshire-county, :common-name "Derbyshire County Council"}
   {:id :derbyshire-dales-district,
    :common-name "Derbyshire Dales District Council"}
   {:id :devon-county, :common-name "Devon County Council"}
   {:id :doncaster-metropolitan-borough,
    :common-name "Doncaster Metropolitan Borough Council"}
   {:id :dorset-county, :common-name "Dorset County Council"}
   {:id :dover-district, :common-name "Dover District Council"}
   {:id :dudley-metropolitan-borough,
    :common-name "Dudley Metropolitan Borough Council"}
   {:id :durham-county, :common-name "Durham County Council"}
   {:id :east-cambridgeshire-district,
    :common-name "East Cambridgeshire District Council"}
   {:id :east-devon-district, :common-name "East Devon District Council"}
   {:id :east-dorset-district, :common-name "East Dorset District Council"}
   {:id :east-hampshire-district,
    :common-name "East Hampshire District Council"}
   {:id :east-hertfordshire-district,
    :common-name "East Hertfordshire District Council"}
   {:id :east-lindsey-district,
    :common-name "East Lindsey District Council"}
   {:id :east-northamptonshire,
    :common-name "East Northamptonshire Council"}
   {:id :east-riding-of-yorkshire,
    :common-name "East Riding of Yorkshire Council"}
   {:id :east-staffordshire-borough,
    :common-name "East Staffordshire Borough Council"}
   {:id :east-suffolk, :common-name "East Suffolk Council"}
   {:id :east-sussex-county, :common-name "East Sussex County Council"}
   {:id :eastbourne-borough, :common-name "Eastbourne Borough Council"}
   {:id :eastleigh-borough, :common-name "Eastleigh Borough Council"}
   {:id :eden-district, :common-name "Eden District Council"}
   {:id :elmbridge-borough, :common-name "Elmbridge Borough Council"}
   {:id :epping-forest-district,
    :common-name "Epping Forest District Council"}
   {:id :epsom-and-ewell-borough,
    :common-name "Epsom and Ewell Borough Council"}
   {:id :erewash-borough, :common-name "Erewash Borough Council"}
   {:id :essex-county, :common-name "Essex County Council"}
   {:id :exeter-city, :common-name "Exeter City Council"}
   {:id :fareham-borough, :common-name "Fareham Borough Council"}
   {:id :fenland-district, :common-name "Fenland District Council"}
   {:id :flintshire-county, :common-name "Flintshire County Council"}
   {:id :forest-heath-district,
    :common-name "Forest Heath District Council"}
   {:id :forest-of-dean-district,
    :common-name "Forest of Dean District Council"}
   {:id :fylde-borough, :common-name "Fylde Borough Council"}
   {:id :gateshead-metropolitan-borough,
    :common-name "Gateshead Metropolitan Borough Council"}
   {:id :gedling-borough, :common-name "Gedling Borough Council"}
   {:id :gloucester-city, :common-name "Gloucester City Council"}
   {:id :gloucestershire-county,
    :common-name "Gloucestershire County Council"}
   {:id :gosport-borough, :common-name "Gosport Borough Council"}
   {:id :gravesham-borough, :common-name "Gravesham Borough Council"}
   {:id :great-yarmouth-borough,
    :common-name "Great Yarmouth Borough Council"}
   {:id :guildford-borough, :common-name "Guildford Borough Council"}
   {:id :gwynedd-county, :common-name "Gwynedd County Council"}
   {:id :halton-borough, :common-name "Halton Borough Council"}
   {:id :hambleton-district, :common-name "Hambleton District Council"}
   {:id :hampshire-county, :common-name "Hampshire County Council"}
   {:id :harborough-district, :common-name "Harborough District Council"}
   {:id :harlow, :common-name "Harlow Council"}
   {:id :harrogate-borough, :common-name "Harrogate Borough Council"}
   {:id :hart-district, :common-name "Hart District Council"}
   {:id :hartlepool-borough, :common-name "Hartlepool Borough Council"}
   {:id :hastings-borough, :common-name "Hastings Borough Council"}
   {:id :havant-borough, :common-name "Havant Borough Council"}
   {:id :herefordshire, :common-name "Herefordshire Council"}
   {:id :hertfordshire-county, :common-name "Hertfordshire County Council"}
   {:id :hertsmere-borough, :common-name "Hertsmere Borough Council"}
   {:id :high-peak-borough, :common-name "High Peak Borough Council"}
   {:id :hinckley-and-bosworth-borough,
    :common-name "Hinckley and Bosworth Borough Council"}
   {:id :horsham-district, :common-name "Horsham District Council"}
   {:id :huntingdonshire-district,
    :common-name "Huntingdonshire District Council"}
   {:id :hyndburn-borough, :common-name "Hyndburn Borough Council"}
   {:id :ipswich-borough, :common-name "Ipswich Borough Council"}
   {:id :isle-of-anglesey-county,
    :common-name "Isle of Anglesey County Council"}
   {:id :isle-of-wight, :common-name "Isle of Wight Council"}
   {:id :isles-of-scilly, :common-name "Isles of Scilly"}
   {:id :kent-county, :common-name "Kent County Council"}
   {:id :kettering-borough, :common-name "Kettering Borough Council"}
   {:id :kings-lynn-and-west-norfolk-borough,
    :common-name "King's Lynn and West Norfolk Borough Council"}
   {:id :kingstonuponhull-city,
    :common-name "Kingston-upon-Hull City Council"}
   {:id :kirklees, :common-name "Kirklees Council"}
   {:id :knowsley-metropolitan-borough,
    :common-name "Knowsley Metropolitan Borough Council"}
   {:id :lancashire-county, :common-name "Lancashire County Council"}
   {:id :lancaster-city, :common-name "Lancaster City Council"}
   {:id :leeds-city, :common-name "Leeds City Council"}
   {:id :leicester-city, :common-name "Leicester City Council"}
   {:id :leicestershire-county,
    :common-name "Leicestershire County Council"}
   {:id :lewes-district, :common-name "Lewes District Council"}
   {:id :lichfield-district, :common-name "Lichfield District Council"}
   {:id :lincolnshire-county, :common-name "Lincolnshire County Council"}
   {:id :liverpool-city, :common-name "Liverpool City Council"}
   {:id :london-borough-of-barking-and-dagenham,
    :common-name "London Borough of Barking and Dagenham"}
   {:id :london-borough-of-barnet, :common-name "London Borough of Barnet"}
   {:id :london-borough-of-bexley, :common-name "London Borough of Bexley"}
   {:id :london-borough-of-brent, :common-name "London Borough of Brent"}
   {:id :london-borough-of-bromley,
    :common-name "London Borough of Bromley"}
   {:id :london-borough-of-camden, :common-name "London Borough of Camden"}
   {:id :london-borough-of-croydon,
    :common-name "London Borough of Croydon"}
   {:id :london-borough-of-ealing, :common-name "London Borough of Ealing"}
   {:id :london-borough-of-enfield,
    :common-name "London Borough of Enfield"}
   {:id :london-borough-of-hackney,
    :common-name "London Borough of Hackney"}
   {:id :london-borough-of-hammersmith--fulham,
    :common-name "London Borough of Hammersmith & Fulham"}
   {:id :london-borough-of-haringey,
    :common-name "London Borough of Haringey"}
   {:id :london-borough-of-harrow, :common-name "London Borough of Harrow"}
   {:id :london-borough-of-havering,
    :common-name "London Borough of Havering"}
   {:id :london-borough-of-hillingdon,
    :common-name "London Borough of Hillingdon"}
   {:id :london-borough-of-hounslow,
    :common-name "London Borough of Hounslow"}
   {:id :london-borough-of-islington,
    :common-name "London Borough of Islington"}
   {:id :london-borough-of-lambeth,
    :common-name "London Borough of Lambeth"}
   {:id :london-borough-of-lewisham,
    :common-name "London Borough of Lewisham"}
   {:id :london-borough-of-merton, :common-name "London Borough of Merton"}
   {:id :london-borough-of-newham, :common-name "London Borough of Newham"}
   {:id :london-borough-of-redbridge,
    :common-name "London Borough of Redbridge"}
   {:id :london-borough-of-richmond-upon-thames,
    :common-name "London Borough of Richmond upon Thames"}
   {:id :london-borough-of-southwark,
    :common-name "London Borough of Southwark"}
   {:id :london-borough-of-sutton, :common-name "London Borough of Sutton"}
   {:id :london-borough-of-tower-hamlets,
    :common-name "London Borough of Tower Hamlets"}
   {:id :london-borough-of-waltham-forest,
    :common-name "London Borough of Waltham Forest"}
   {:id :london-borough-of-wandsworth,
    :common-name "London Borough of Wandsworth"}
   {:id :luton-borough, :common-name "Luton Borough Council"}
   {:id :maidstone-borough, :common-name "Maidstone Borough Council"}
   {:id :maldon-district, :common-name "Maldon District Council"}
   {:id :malvern-hills-district,
    :common-name "Malvern Hills District Council"}
   {:id :manchester-city, :common-name "Manchester City Council"}
   {:id :mansfield-district, :common-name "Mansfield District Council"}
   {:id :medway, :common-name "Medway Council"}
   {:id :melton-borough, :common-name "Melton Borough Council"}
   {:id :mendip-district, :common-name "Mendip District Council"}
   {:id :merthyr-tydfil-county-borough,
    :common-name "Merthyr Tydfil County Borough Council"}
   {:id :mid-devon-district, :common-name "Mid Devon District Council"}
   {:id :mid-suffolk-district, :common-name "Mid Suffolk District Council"}
   {:id :mid-sussex-district, :common-name "Mid Sussex District Council"}
   {:id :middlesbrough-borough,
    :common-name "Middlesbrough Borough Council"}
   {:id :milton-keynes, :common-name "Milton Keynes"}
   {:id :mole-valley-district, :common-name "Mole Valley District Council"}
   {:id :monmouthshire-county, :common-name "Monmouthshire County Council"}
   {:id :neath-port-talbot-county-borough,
    :common-name "Neath Port Talbot County Borough Council"}
   {:id :new-forest-district, :common-name "New Forest District Council"}
   {:id :newark-and-sherwood-district,
    :common-name "Newark and Sherwood District Council"}
   {:id :newcastleunderlyme-district,
    :common-name "Newcastle-Under-Lyme District Council"}
   {:id :newport-city, :common-name "Newport City Council"}
   {:id :newcastleupontyne-city,
    :common-name "Newcastle-upon-Tyne City Council"}
   {:id :norfolk-county, :common-name "Norfolk County Council"}
   {:id :north-devon, :common-name "North Devon Council"}
   {:id :north-dorset-district,
    :common-name "North Dorset District Council"}
   {:id :north-east-derbyshire-district,
    :common-name "North East Derbyshire District Council"}
   {:id :north-east-lincolnshire,
    :common-name "North East Lincolnshire Council"}
   {:id :north-hertfordshire-district,
    :common-name "North Hertfordshire District Council"}
   {:id :north-kesteven-district,
    :common-name "North Kesteven District Council"}
   {:id :north-lincolnshire, :common-name "North Lincolnshire Council"}
   {:id :north-norfolk-district,
    :common-name "North Norfolk District Council"}
   {:id :north-somerset, :common-name "North Somerset Council"}
   {:id :north-tyneside-metropolitan-borough,
    :common-name "North Tyneside Metropolitan Borough Council"}
   {:id :north-warwickshire-borough,
    :common-name "North Warwickshire Borough Council"}
   {:id :north-west-leicestershire-district,
    :common-name "North West Leicestershire District Council"}
   {:id :north-yorkshire-county,
    :common-name "North Yorkshire County Council"}
   {:id :northampton-borough, :common-name "Northampton Borough Council"}
   {:id :northamptonshire-county,
    :common-name "Northamptonshire County Council"}
   {:id :northumberland, :common-name "Northumberland Council"}
   {:id :norwich-city, :common-name "Norwich City Council"}
   {:id :nottingham-city, :common-name "Nottingham City Council"}
   {:id :nottinghamshire-county,
    :common-name "Nottinghamshire County Council"}
   {:id :nuneaton-and-bedworth-borough,
    :common-name "Nuneaton and Bedworth Borough Council"}
   {:id :oadby-and-wigston-district,
    :common-name "Oadby and Wigston District Council"}
   {:id :oldham-metropolitan-borough,
    :common-name "Oldham Metropolitan Borough Council"}
   {:id :oxford-city, :common-name "Oxford City Council"}
   {:id :oxfordshire-county, :common-name "Oxfordshire County Council"}
   {:id :pembrokeshire-county, :common-name "Pembrokeshire County Council"}
   {:id :pendle-borough, :common-name "Pendle Borough Council"}
   {:id :perth-and-kinross, :common-name "Perth and Kinross Council"}
   {:id :peterborough-city, :common-name "Peterborough City Council"}
   {:id :plymouth-city, :common-name "Plymouth City Council"}
   {:id :portsmouth-city, :common-name "Portsmouth City Council"}
   {:id :powys-county, :common-name "Powys County Council"}
   {:id :preston-city, :common-name "Preston City Council"}
   {:id :purbeck-district, :common-name "Purbeck District Council"}
   {:id :reading-borough, :common-name "Reading Borough Council"}
   {:id :redcar-and-cleveland, :common-name "Redcar and Cleveland Council"}
   {:id :redditch-borough, :common-name "Redditch Borough Council"}
   {:id :reigate--banstead-borough,
    :common-name "Reigate & Banstead Borough Council"}
   {:id :rhondda-cynon-taf-county-borough,
    :common-name "Rhondda Cynon Taf County Borough Council"}
   {:id :ribble-valley-borough,
    :common-name "Ribble Valley Borough Council"}
   {:id :richmondshire-district,
    :common-name "Richmondshire District Council"}
   {:id :rochdale-metropolitan-borough,
    :common-name "Rochdale Metropolitan Borough Council"}
   {:id :rochford-district, :common-name "Rochford District Council"}
   {:id :rossendale-borough, :common-name "Rossendale Borough Council"}
   {:id :rother-district, :common-name "Rother District Council"}
   {:id :rotherham-metropolitan-borough,
    :common-name "Rotherham Metropolitan Borough Council"}
   {:id :royal-borough-of-greenwich,
    :common-name "Royal Borough of Greenwich"}
   {:id :royal-borough-of-kensington-and-chelsea,
    :common-name "Royal Borough of Kensington and Chelsea"}
   {:id :royal-borough-of-kingston-upon-thames,
    :common-name "Royal Borough of Kingston upon Thames"}
   {:id :royal-borough-of-windsor-and-maidenhead,
    :common-name "Royal Borough of Windsor and Maidenhead"}
   {:id :rugby-borough, :common-name "Rugby Borough Council"}
   {:id :runnymede-borough, :common-name "Runnymede Borough Council"}
   {:id :rushcliffe-borough, :common-name "Rushcliffe Borough Council"}
   {:id :rushmoor-borough, :common-name "Rushmoor Borough Council"}
   {:id :rutland-county, :common-name "Rutland County Council"}
   {:id :ryedale-district, :common-name "Ryedale District Council"}
   {:id :salford-city, :common-name "Salford City Council"}
   {:id :sandwell-metropolitan-borough,
    :common-name "Sandwell Metropolitan Borough Council"}
   {:id :scarborough-borough, :common-name "Scarborough Borough Council"}
   {:id :sedgemoor-district, :common-name "Sedgemoor District Council"}
   {:id :sefton-metropolitan-borough,
    :common-name "Sefton Metropolitan Borough Council"}
   {:id :selby-district, :common-name "Selby District Council"}
   {:id :sevenoaks-district, :common-name "Sevenoaks District Council"}
   {:id :sheffield-city, :common-name "Sheffield City Council"}
   {:id :shepway-district, :common-name "Shepway District Council"}
   {:id :shropshire--unitary, :common-name "Shropshire Council - Unitary"}
   {:id :slough-borough, :common-name "Slough Borough Council"}
   {:id :solihull-metropolitan-borough,
    :common-name "Solihull Metropolitan Borough Council"}
   {:id :somerset-county, :common-name "Somerset County Council"}
   {:id :south-buckinghamshire-district,
    :common-name "South Buckinghamshire District Council"}
   {:id :south-cambridgeshire-district,
    :common-name "South Cambridgeshire District Council"}
   {:id :south-derbyshire-district,
    :common-name "South Derbyshire District Council"}
   {:id :south-gloucestershire,
    :common-name "South Gloucestershire Council"}
   {:id :south-hams-district, :common-name "South Hams District Council"}
   {:id :south-holland-district,
    :common-name "South Holland District Council"}
   {:id :south-kesteven-district,
    :common-name "South Kesteven District Council"}
   {:id :south-lakeland-district,
    :common-name "South Lakeland District Council"}
   {:id :south-norfolk-district,
    :common-name "South Norfolk District Council"}
   {:id :south-northamptonshire,
    :common-name "South Northamptonshire Council"}
   {:id :south-oxfordshire-district,
    :common-name "South Oxfordshire District Council"}
   {:id :south-ribble-borough, :common-name "South Ribble Borough Council"}
   {:id :south-somerset-district,
    :common-name "South Somerset District Council"}
   {:id :south-staffordshire, :common-name "South Staffordshire Council"}
   {:id :south-tyneside, :common-name "South Tyneside Council"}
   {:id :southampton-city, :common-name "Southampton City Council"}
   {:id :southendonsea-borough,
    :common-name "Southend-on-Sea Borough Council"}
   {:id :spelthorne-borough, :common-name "Spelthorne Borough Council"}
   {:id :st-albans-city-and-district,
    :common-name "St Albans City and District Council"}
   {:id :st-edmundsbury-borough,
    :common-name "St Edmundsbury Borough Council"}
   {:id :st-helens-metropolitan-borough,
    :common-name "St Helens Metropolitan Borough Council"}
   {:id :stafford-borough, :common-name "Stafford Borough Council"}
   {:id :staffordshire-county, :common-name "Staffordshire County Council"}
   {:id :staffordshire-moorlands-district,
    :common-name "Staffordshire Moorlands District Council"}
   {:id :stevenage-borough, :common-name "Stevenage Borough Council"}
   {:id :stockport-metropolitan-borough,
    :common-name "Stockport Metropolitan Borough Council"}
   {:id :stocktonontees-borough,
    :common-name "Stockton-on-Tees Borough Council"}
   {:id :stokeontrent-city, :common-name "Stoke-on-Trent City Council"}
   {:id :strabane-district, :common-name "Strabane District Council"}
   {:id :stratfordonavon-district,
    :common-name "Stratford-on-Avon District Council"}
   {:id :stroud-district, :common-name "Stroud District Council"}
   {:id :suffolk-county, :common-name "Suffolk County Council"}
   {:id :sunderland-city, :common-name "Sunderland City Council"}
   {:id :surrey-county, :common-name "Surrey County Council"}
   {:id :surrey-heath-borough, :common-name "Surrey Heath Borough Council"}
   {:id :swale-borough, :common-name "Swale Borough Council"}
   {:id :swansea-city-and-borough,
    :common-name "Swansea City and Borough Council"}
   {:id :swindon-borough, :common-name "Swindon Borough Council"}
   {:id :tameside-metropolitan-borough,
    :common-name "Tameside Metropolitan Borough Council"}
   {:id :tamworth-borough, :common-name "Tamworth Borough Council"}
   {:id :tandridge-district, :common-name "Tandridge District Council"}
   {:id :taunton-deane-borough,
    :common-name "Taunton Deane Borough Council"}
   {:id :teignbridge-district, :common-name "Teignbridge District Council"}
   {:id :telford--wrekin, :common-name "Telford & Wrekin Council"}
   {:id :tendring-district, :common-name "Tendring District Council"}
   {:id :test-valley-borough, :common-name "Test Valley Borough Council"}
   {:id :tewkesbury-borough, :common-name "Tewkesbury Borough Council"}
   {:id :thanet-district, :common-name "Thanet District Council"}
   {:id :three-rivers-district,
    :common-name "Three Rivers District Council"}
   {:id :thurrock, :common-name "Thurrock Council"}
   {:id :tonbridge-and-malling-borough,
    :common-name "Tonbridge and Malling Borough Council"}
   {:id :torbay, :common-name "Torbay Council"}
   {:id :torfaen-county-borough,
    :common-name "Torfaen County Borough Council"}
   {:id :torridge-district, :common-name "Torridge District Council"}
   {:id :trafford-metropolitan-borough,
    :common-name "Trafford Metropolitan Borough Council"}
   {:id :tunbridge-wells-borough,
    :common-name "Tunbridge Wells Borough Council"}
   {:id :uttlesford-district, :common-name "Uttlesford District Council"}
   {:id :vale-of-glamorgan, :common-name "Vale of Glamorgan Council"}
   {:id :vale-of-white-horse-district,
    :common-name "Vale of White Horse District Council"}
   {:id :wakefield-metropolitan-district,
    :common-name "Wakefield Metropolitan District Council"}
   {:id :walsall-metropolitan-borough,
    :common-name "Walsall Metropolitan Borough Council"}
   {:id :warrington-borough, :common-name "Warrington Borough Council"}
   {:id :warwick-district, :common-name "Warwick District Council"}
   {:id :warwickshire-county, :common-name "Warwickshire County Council"}
   {:id :watford-borough, :common-name "Watford Borough Council"}
   {:id :waverley-borough, :common-name "Waverley Borough Council"}
   {:id :wealden-district, :common-name "Wealden District Council"}
   {:id :wellingborough-borough,
    :common-name "Wellingborough Borough Council"}
   {:id :welwyn-hatfield, :common-name "Welwyn Hatfield Council"}
   {:id :west-berkshire, :common-name "West Berkshire Council"}
   {:id :west-devon-borough, :common-name "West Devon Borough Council"}
   {:id :west-dorset-district, :common-name "West Dorset District Council"}
   {:id :west-lancashire-borough,
    :common-name "West Lancashire Borough Council"}
   {:id :west-lindsey-district,
    :common-name "West Lindsey District Council"}
   {:id :west-oxfordshire-district,
    :common-name "West Oxfordshire District Council"}
   {:id :west-somerset-district,
    :common-name "West Somerset District Council"}
   {:id :west-sussex-county, :common-name "West Sussex County Council"}
   {:id :westminster-city, :common-name "Westminster City Council"}
   {:id :weymouth-and-portland-borough,
    :common-name "Weymouth and Portland Borough Council"}
   {:id :wigan-metropolitan-borough,
    :common-name "Wigan Metropolitan Borough Council"}
   {:id :wiltshire, :common-name "Wiltshire Council"}
   {:id :winchester-city, :common-name "Winchester City Council"}
   {:id :wirral, :common-name "Wirral Council"}
   {:id :woking-borough, :common-name "Woking Borough Council"}
   {:id :wokingham-borough, :common-name "Wokingham Borough Council"}
   {:id :wolverhampton-city, :common-name "Wolverhampton City Council"}
   {:id :worcester-city, :common-name "Worcester City Council"}
   {:id :worcestershire-county,
    :common-name "Worcestershire County Council"}
   {:id :worthing-borough, :common-name "Worthing Borough Council"}
   {:id :wrexham-county-borough,
    :common-name "Wrexham County Borough Council"}
   {:id :wychavon-district, :common-name "Wychavon District Council"}
   {:id :wycombe-district, :common-name "Wycombe District Council"}
   {:id :wyre, :common-name "Wyre Council"}
   {:id :wyre-forest-district, :common-name "Wyre Forest District Council"}])

(def council-addresses-e-and-w
  [{:common-name "Adur and Worthing Borough Council",
    :address-1 "Portland House",
    :address-2 "Richmond Road",
    :town "Worthing",
    :county "West Sussex",
    :postcode "BN11 1HS",
    :id :adur-and-worthing-borough}
   {:common-name "Adur District Council",
    :address-1 "Churchill House",
    :address-2 "Churchill Square",
    :town "Brighton",
    :county "East Sussex",
    :postcode "BN1 2AF",
    :id :adur-district}
   {:common-name "Allerdale Borough Council",
    :address-1 "Allerdale House",
    :address-2 "New Bridge Road",
    :town "Workington",
    :county "Cumbria",
    :postcode "CA14 3YJ",
    :id :allerdale-borough}
   {:common-name "Amber Valley Borough Council",
    :address-1 "Town Hall",
    :address-2 "Ripley",
    :town "Derbyshire",
    :county "Derbyshire",
    :postcode "DE5 3BT",
    :id :amber-valley-borough}
   {:common-name "Arun District Council",
    :address-1 "Arun Civic Centre",
    :address-2 "Maltravers Road",
    :town "Littlehampton",
    :county "West Sussex",
    :postcode "BN17 5LF",
    :id :arun-district}
   {:common-name "Ashfield District Council",
    :address-1 "Urban Road",
    :address-2 "Kirkby-in-Ashfield",
    :town "Nottinghamshire",
    :county "Nottinghamshire",
    :postcode "NG17 8DA",
    :id :ashfield-district}
   {:common-name "Ashford Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Tannery Lane",
    :town "Ashford",
    :county "Kent",
    :postcode "TN23 1PL",
    :id :ashford-borough}
   {:common-name "Aylesbury Vale District Council",
    :address-1 "The Gateway",
    :address-2 "Gatehouse Road",
    :town "Aylesbury",
    :county "Buckinghamshire",
    :postcode "HP19 8FF",
    :id :aylesbury-vale-district}
   {:common-name "Babergh District Council",
    :address-1 "Corks Lane",
    :address-2 "Hadleigh",
    :town "Ipswich",
    :county "Suffolk",
    :postcode "IP7 6SJ",
    :id :babergh-district}
   {:common-name "Barnsley Metropolitan Borough Council",
    :address-1 "Town Hall",
    :address-2 "Church Street",
    :town "Barnsley",
    :county "South Yorkshire",
    :postcode "S70 2TA",
    :id :barnsley-metropolitan-borough}
   {:common-name "Barrow-in-Furness Borough Council",
    :address-1 "Town Hall",
    :address-2 "Duke Street",
    :town "Barrow-in-Furness",
    :county "Cumbria",
    :postcode "LA14 2LD",
    :id :barrowinfurness-borough}
   {:common-name "Basildon Borough Council",
    :address-1 "The Basildon Centre",
    :address-2 "St. Martin's Square",
    :town "Basildon",
    :county "Essex",
    :postcode "SS14 1DL",
    :id :basildon-borough}
   {:common-name "Basingstoke and Deane Borough Council",
    :address-1 "Civic Offices",
    :address-2 "London Road",
    :town "Basingstoke",
    :county "Hampshire",
    :postcode "RG21 4AH",
    :id :basingstoke-and-deane-borough}
   {:common-name "Bassetlaw District Council",
    :address-1 "Queen's Buildings",
    :address-2 "Potter Street",
    :town "Worksop",
    :county "Nottinghamshire",
    :postcode "S80 2AH",
    :id :bassetlaw-district}
   {:common-name "Bath and North East Somerset Council",
    :address-1 "Lewis House",
    :address-2 "Manvers Street",
    :town "Bath",
    :county "Somerset",
    :postcode "BA1 1JG",
    :id :bath-and-north-east-somerset}
   {:common-name "Bedford Borough Council",
    :address-1 "Borough Hall",
    :address-2 "Cauldwell Street",
    :town "Bedford",
    :county "Bedfordshire",
    :postcode "MK42 9AP",
    :id :bedford-borough}
   {:common-name "Birmingham City Council",
    :address-1 "Council House",
    :address-2 "Victoria Square",
    :town "Birmingham",
    :county "West Midlands",
    :postcode "B1 1BB",
    :id :birmingham-city}
   {:common-name "Blaby District Council",
    :address-1 "Desford Road",
    :address-2 "Narborough",
    :town "Leicester",
    :county "Leicestershire",
    :postcode "LE19 2EP",
    :id :blaby-district}
   {:common-name "Blackburn with Darwen Borough Council",
    :address-1 "Town Hall",
    :address-2 "King William Street",
    :town "Blackburn",
    :county "Lancashire",
    :postcode "BB1 7DY",
    :id :blackburn-with-darwen-borough}
   {:common-name "Blackpool Borough Council",
    :address-1 "Town Hall",
    :address-2 "Talbot Road",
    :town "Blackpool",
    :county "Lancashire",
    :postcode "FY1 1AD",
    :id :blackpool-borough}
   {:common-name "Blaenau Gwent County Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Ebbw Vale",
    :town "Gwent",
    :county "Wales",
    :postcode "NP23 6XB",
    :id :blaenau-gwent-county-borough}
   {:common-name "Bolsover District Council",
    :address-1 "The Arc",
    :address-2 "High Street",
    :town "Clowne",
    :county "Derbyshire",
    :postcode "S43 4JY",
    :id :bolsover-district}
   {:common-name "Bolton Metropolitan Borough Council",
    :address-1 "Bolton Town Hall",
    :address-2 "Victoria Square",
    :town "Bolton",
    :county "Greater Manchester",
    :postcode "BL1 1RU",
    :id :bolton-metropolitan-borough}
   {:common-name "Borough of Broxbourne",
    :address-1 "Bishops' College",
    :address-2 "Churchgate",
    :town "Cheshunt",
    :county "Hertfordshire",
    :postcode "EN8 9XQ",
    :id :borough-of-broxbourne}
   {:common-name "Borough of Poole",
    :address-1 "Civic Centre",
    :address-2 "Poole",
    :town "Dorset",
    :county "England",
    :postcode "BH15 2RU",
    :id :borough-of-poole}
   {:common-name "Boston Borough Council",
    :address-1 "Municipal Buildings",
    :address-2 "West Street",
    :town "Boston",
    :county "Lincolnshire",
    :postcode "PE21 8QR",
    :id :boston-borough}
   {:common-name "Bournemouth Borough Council",
    :address-1 "Town Hall",
    :address-2 "Bourne Avenue",
    :town "Bournemouth",
    :county "Dorset",
    :postcode "BH2 6DY",
    :id :bournemouth-borough}
   {:common-name "Bracknell Forest Council",
    :address-1 "Time Square",
    :address-2 "Market Street",
    :town "Bracknell",
    :county "Berkshire",
    :postcode "RG12 1JD",
    :id :bracknell-forest}
   {:common-name "Bradford Metropolitan District Council",
    :address-1 "City Hall",
    :address-2 "Centenary Square",
    :town "Bradford",
    :county "West Yorkshire",
    :postcode "BD1 1HY",
    :id :bradford-metropolitan-district}
   {:common-name "Braintree District Council",
    :address-1 "Causeway House",
    :address-2 "Bocking End",
    :town "Braintree",
    :county "Essex",
    :postcode "CM7 9HB",
    :id :braintree-district}
   {:common-name "Breckland District Council",
    :address-1 "Elizabeth House",
    :address-2 "Walpole Loke",
    :town "Dereham",
    :county "Norfolk",
    :postcode "NR19 1EE",
    :id :breckland-district}
   {:common-name "Brentwood Borough Council",
    :address-1 "Town Hall",
    :address-2 "Ingrave Road",
    :town "Brentwood",
    :county "Essex",
    :postcode "CM15 8AY",
    :id :brentwood-borough}
   {:common-name "Bridgend County Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Angel Street",
    :town "Bridgend",
    :county "Mid Glamorgan",
    :postcode "CF31 4WB",
    :id :bridgend-county-borough}
   {:common-name "Brighton and Hove City Council",
    :address-1 "King's House",
    :address-2 "Grand Avenue",
    :town "Hove",
    :county "East Sussex",
    :postcode "BN3 2LS",
    :id :brighton-and-hove-city}
   {:common-name "Bristol City Council",
    :address-1 "City Hall",
    :address-2 "College Green",
    :town "Bristol",
    :county "Avon",
    :postcode "BS1 5TR",
    :id :bristol-city}
   {:common-name "Broadland District Council",
    :address-1 "Thorpe Lodge",
    :address-2 "1 Yarmouth Road",
    :town "Norwich",
    :county "Norfolk",
    :postcode "NR7 0DU",
    :id :broadland-district}
   {:common-name "Bromsgrove District Council",
    :address-1 "Parkside",
    :address-2 "Market Street",
    :town "Bromsgrove",
    :county "Worcestershire",
    :postcode "B61 8DA",
    :id :bromsgrove-district}
   {:common-name "Broxtowe Borough Council",
    :address-1 "Foster Avenue",
    :address-2 "Beeston",
    :town "Nottingham",
    :county "Nottinghamshire",
    :postcode "NG9 1AB",
    :id :broxtowe-borough}
   {:common-name "Buckinghamshire County Council",
    :address-1 "County Hall",
    :address-2 "Walton Street",
    :town "Aylesbury",
    :county "Buckinghamshire",
    :postcode "HP20 1UA",
    :id :buckinghamshire-county}
   {:common-name "Burnley Borough Council",
    :address-1 "Town Hall",
    :address-2 "Manchester Road",
    :town "Burnley",
    :county "Lancashire",
    :postcode "BB11 1JA",
    :id :burnley-borough}
   {:common-name "Bury Metropolitan Borough Council",
    :address-1 "Town Hall",
    :address-2 "Knowsley Street",
    :town "Bury",
    :county "Greater Manchester",
    :postcode "BL9 0SW",
    :id :bury-metropolitan-borough}
   {:common-name "Caerphilly County Borough Council",
    :address-1 "Penallta House",
    :address-2 "Tredomen Park",
    :town "Ystrad Mynach",
    :county "Caerphilly",
    :postcode "CF82 7PG",
    :id :caerphilly-county-borough}
   {:common-name "Calderdale Metropolitan Borough Council",
    :address-1 "Halifax Town Hall",
    :address-2 "Crossley Street",
    :town "Halifax",
    :county "West Yorkshire",
    :postcode "HX1 1UJ",
    :id :calderdale-metropolitan-borough}
   {:common-name "Cambridge City Council",
    :address-1 "The Guildhall",
    :address-2 "Market Square",
    :town "Cambridge",
    :county "Cambridgeshire",
    :postcode "CB2 3QJ",
    :id :cambridge-city}
   {:common-name "Cambridgeshire County Council",
    :address-1 "Shire Hall",
    :address-2 "Castle Hill",
    :town "Cambridge",
    :county "Cambridgeshire",
    :postcode "CB3 0AP",
    :id :cambridgeshire-county}
   {:common-name "Cannock Chase District Council",
    :address-1 "Civic Centre",
    :address-2 "Beecroft Road",
    :town "Cannock",
    :county "Staffordshire",
    :postcode "WS11 1BG",
    :id :cannock-chase-district}
   {:common-name "Canterbury City Council",
    :address-1 "Military Road Offices",
    :address-2 "Military Road",
    :town "Canterbury",
    :county "Kent",
    :postcode "CT1 1YW",
    :id :canterbury-city}
   {:common-name "Cardiff Council",
    :address-1 "County Hall",
    :address-2 "Atlantic Wharf",
    :town "Cardiff",
    :county "South Glamorgan",
    :postcode "CF10 4UW",
    :id :cardiff}
   {:common-name "Carlisle City Council",
    :address-1 "Civic Centre",
    :address-2 "Rickergate",
    :town "Carlisle",
    :county "Cumbria",
    :postcode "CA3 8QG",
    :id :carlisle-city}
   {:common-name "Carmarthenshire County Council",
    :address-1 "County Hall",
    :address-2 "Picton Terrace",
    :town "Carmarthen",
    :county "Carmarthenshire",
    :postcode "SA31 1JP",
    :id :carmarthenshire-county}
   {:common-name "Castle Point Borough Council",
    :address-1 "Kiln Road",
    :address-2 "Thundersley",
    :town "Benfleet",
    :county "Essex",
    :postcode "SS7 1TF",
    :id :castle-point-borough}
   {:common-name "Central Bedfordshire Council",
    :address-1 "Priory House",
    :address-2 "Monks Walk",
    :town "Chicksands",
    :county "Bedfordshire",
    :postcode "SG17 5TQ",
    :id :central-bedfordshire}
   {:common-name "Ceredigion County Council",
    :address-1 "Canolfan Rheidol",
    :address-2 "Rhodfa Padarn",
    :town "Aberystwyth",
    :county "Ceredigion",
    :postcode "SY23 3UE",
    :id :ceredigion-county}
   {:common-name "Charnwood Borough Council",
    :address-1 "Southfield Road",
    :address-2 "",
    :town "Loughborough",
    :county "Leicestershire",
    :postcode "LE11 2TX",
    :id :charnwood-borough}
   {:common-name "Chelmsford City Council",
    :address-1 "Civic Centre",
    :address-2 "Duke Street",
    :town "Chelmsford",
    :county "Essex",
    :postcode "CM1 1JE",
    :id :chelmsford-city}
   {:common-name "Cheltenham Borough Council",
    :address-1 "Municipal Offices",
    :address-2 "Promenade",
    :town "Cheltenham",
    :county "Gloucestershire",
    :postcode "GL50 9SA",
    :id :cheltenham-borough}
   {:common-name "Cherwell District Council",
    :address-1 "Bodicote House",
    :address-2 "Bodicote",
    :town "Banbury",
    :county "Oxfordshire",
    :postcode "OX15 4AA",
    :id :cherwell-district}
   {:common-name "Cheshire East Council",
    :address-1 "Westfields",
    :address-2 "Middlewich Road",
    :town "Sandbach",
    :county "Cheshire",
    :postcode "CW11 1HZ",
    :id :cheshire-east}
   {:common-name "Cheshire West and Chester Council",
    :address-1 "HQ",
    :address-2 "58 Nicholas Street",
    :town "Chester",
    :county "Cheshire",
    :postcode "CH1 2NP",
    :id :cheshire-west-and-chester}
   {:common-name "Chesterfield Borough Council",
    :address-1 "Town Hall",
    :address-2 "Rose Hill",
    :town "Chesterfield",
    :county "Derbyshire",
    :postcode "S40 1LP",
    :id :chesterfield-borough}
   {:common-name "Chichester District Council",
    :address-1 "East Pallant House",
    :address-2 "1 East Pallant",
    :town "Chichester",
    :county "West Sussex",
    :postcode "PO19 1TY",
    :id :chichester-district}
   {:common-name "Chiltern District Council",
    :address-1 "King George V House",
    :address-2 "King George V Road",
    :town "Amersham",
    :county "Buckinghamshire",
    :postcode "HP6 5AW",
    :id :chiltern-district}
   {:common-name "Chorley Council",
    :address-1 "Town Hall",
    :address-2 "Market Street",
    :town "Chorley",
    :county "Lancashire",
    :postcode "PR7 1DP",
    :id :chorley}
   {:common-name "Christchurch Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Bridge Street",
    :town "Christchurch",
    :county "Dorset",
    :postcode "BH23 1AZ",
    :id :christchurch-borough}
   {:common-name "City of Lincoln Council",
    :address-1 "City Hall",
    :address-2 "Beaumont Fee",
    :town "Lincoln",
    :county "Lincolnshire",
    :postcode "LN1 1DD",
    :id :city-of-lincoln}
   {:common-name "City of London",
    :address-1 "Guildhall",
    :address-2 "Gresham Street",
    :town "London",
    :postcode "EC2V 7HH",
    :id :city-of-london}
   {:common-name "City of York Council",
    :address-1 "West Offices",
    :address-2 "Station Rise",
    :town "York",
    :county "North Yorkshire",
    :postcode "YO1 6GA",
    :id :city-of-york}
   {:common-name "Colchester Borough Council",
    :address-1 "Rowan House",
    :address-2 "33 Sheepen Road",
    :town "Colchester",
    :county "Essex",
    :postcode "CO3 3WG",
    :id :colchester-borough}
   {:common-name "Conwy County Borough Council",
    :address-1 "Bodlondeb",
    :address-2 "Conwy",
    :town "Conwy",
    :county "Conwy",
    :postcode "LL32 8DU",
    :id :conwy-county-borough}
   {:common-name "Copeland Borough Council",
    :address-1 "The Copeland Centre",
    :address-2 "Catherine Street",
    :town "Whitehaven",
    :county "Cumbria",
    :postcode "CA28 7SJ",
    :id :copeland-borough}
   {:common-name "Corby Borough Council",
    :address-1 "The Corby Cube",
    :address-2 "Parkland Gateway",
    :town "George Street",
    :county "Northamptonshire",
    :postcode "NN17 1QG",
    :id :corby-borough}
   {:common-name "Cornwall Council (Unitary)",
    :address-1 "New County Hall",
    :address-2 "Treyew Road",
    :town "Truro",
    :county "Cornwall",
    :postcode "TR1 3AY",
    :id :cornwall-unitary}
   {:common-name "Cotswold District Council",
    :address-1 "Trinity Road",
    :town "Cirencester",
    :county "Gloucestershire",
    :postcode "GL7 1PX",
    :id :cotswold-district}
   {:common-name "Coventry City Council",
    :address-1 "Council House",
    :address-2 "Earl Street",
    :town "Coventry",
    :county "West Midlands",
    :postcode "CV1 5RR",
    :id :coventry-city}
   {:common-name "Craven District Council",
    :address-1 "1 Belle Vue Square",
    :town "Broughton Road, Skipton",
    :county "North Yorkshire",
    :postcode "BD23 1FJ",
    :id :craven-district}
   {:common-name "Crawley Borough Council",
    :address-1 "Town Hall",
    :address-2 "The Boulevard",
    :town "Crawley",
    :county "West Sussex",
    :postcode "RH10 1UZ",
    :id :crawley-borough}
   {:common-name "Cumbria County Council",
    :address-1 "Cumbria House",
    :address-2 "117 Botchergate",
    :town "Carlisle",
    :county "Cumbria",
    :postcode "CA1 1RD",
    :id :cumbria-county}
   {:common-name "Dacorum Borough Council",
    :address-1 "The Forum",
    :address-2 "Marlowes",
    :town "Hemel Hempstead",
    :county "Hertfordshire",
    :postcode "HP1 1DN",
    :id :dacorum-borough}
   {:common-name "Darlington Borough Council",
    :address-1 "Town Hall",
    :address-2 "Feethams",
    :town "Darlington",
    :county "County Durham",
    :postcode "DL1 5QT",
    :id :darlington-borough}
   {:common-name "Dartford Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Home Gardens",
    :town "Dartford",
    :county "Kent",
    :postcode "DA1 1DR",
    :id :dartford-borough}
   {:common-name "Daventry District Council",
    :address-1 "Lodge Road",
    :town "Daventry",
    :county "Northamptonshire",
    :postcode "NN11 4FP",
    :id :daventry-district}
   {:common-name "Denbighshire County Council",
    :address-1 "County Hall",
    :address-2 "Wynnstay Road",
    :town "Ruthin",
    :county "Denbighshire",
    :postcode "LL15 1YN",
    :id :denbighshire-county}
   {:common-name "Derby City Council",
    :address-1 "The Council House",
    :address-2 "Corporation Street",
    :town "Derby",
    :county "Derbyshire",
    :postcode "DE1 2FS",
    :id :derby-city}
   {:common-name "Derbyshire County Council",
    :address-1 "County Hall",
    :address-2 "Matlock",
    :town "Derbyshire",
    :county "Derbyshire",
    :postcode "DE4 3AG",
    :id :derbyshire-county}
   {:common-name "Derbyshire Dales District Council",
    :address-1 "Town Hall",
    :address-2 "Bank Road",
    :town "Matlock",
    :county "Derbyshire",
    :postcode "DE4 3NN",
    :id :derbyshire-dales-district}
   {:common-name "Devon County Council",
    :address-1 "County Hall",
    :address-2 "Topsham Road",
    :town "Exeter",
    :county "Devon",
    :postcode "EX2 4QD",
    :id :devon-county}
   {:common-name "Doncaster Metropolitan Borough Council",
    :address-1 "Civic Office",
    :address-2 "Waterdale",
    :town "Doncaster",
    :county "South Yorkshire",
    :postcode "DN1 3BU",
    :id :doncaster-metropolitan-borough}
   {:common-name "Dorset County Council",
    :address-1 "County Hall",
    :address-2 "Colliton Park",
    :town "Dorchester",
    :county "Dorset",
    :postcode "DT1 1XJ",
    :id :dorset-county}
   {:common-name "Dover District Council",
    :address-1 "Council Offices",
    :address-2 "White Cliffs Business Park",
    :town "Dover",
    :county "Kent",
    :postcode "CT16 3PJ",
    :id :dover-district}
   {:common-name "Dudley Metropolitan Borough Council",
    :address-1 "Dudley Council House",
    :address-2 "Priory Road",
    :town "Dudley",
    :county "West Midlands",
    :postcode "DY1 1HF",
    :id :dudley-metropolitan-borough}
   {:common-name "Durham County Council",
    :address-1 "County Hall",
    :address-2 "Durham",
    :town "Durham",
    :county "County Durham",
    :postcode "DH1 5UL",
    :id :durham-county}
   {:common-name "East Cambridgeshire District Council",
    :address-1 "The Grange",
    :address-2 "Nutholt Lane",
    :town "Ely",
    :county "Cambridgeshire",
    :postcode "CB7 4EE",
    :id :east-cambridgeshire-district}
   {:common-name "East Devon District Council",
    :address-1 "Blackdown House",
    :address-2 "Heathpark Way",
    :town "Honiton",
    :county "Devon",
    :postcode "EX14 1EJ",
    :id :east-devon-district}
   {:common-name "East Dorset District Council",
    :address-1 "Furzehill",
    :address-2 "Wimborne",
    :town "Dorset",
    :county "Dorset",
    :postcode "BH21 4HN",
    :id :east-dorset-district}
   {:common-name "East Hampshire District Council",
    :address-1 "Penns Place",
    :address-2 "Petersfield",
    :town "Hampshire",
    :county "Hampshire",
    :postcode "GU31 4EX",
    :id :east-hampshire-district}
   {:common-name "East Hertfordshire District Council",
    :address-1 "Wallfields",
    :address-2 "Pegs Lane",
    :town "Hertford",
    :county "Hertfordshire",
    :postcode "SG13 8EQ",
    :id :east-hertfordshire-district}
   {:common-name "East Lindsey District Council",
    :address-1 "Tedder Hall",
    :address-2 "Manby Park",
    :town "Louth",
    :county "Lincolnshire",
    :postcode "LN11 8UP",
    :id :east-lindsey-district}
   {:common-name "East Northamptonshire Council",
    :address-1 "Cedar Drive",
    :address-2 "Thrapston",
    :town "Kettering",
    :county "Northamptonshire",
    :postcode "NN14 4LZ",
    :id :east-northamptonshire}
   {:common-name "East Riding of Yorkshire Council",
    :address-1 "County Hall",
    :address-2 "Cross Street",
    :town "Beverley",
    :county "East Riding of Yorkshire",
    :postcode "HU17 9BA",
    :id :east-riding-of-yorkshire}
   {:common-name "East Staffordshire Borough Council",
    :address-1 "The Maltsters",
    :address-2 "Wetmore Road",
    :town "Burton upon Trent",
    :county "Staffordshire",
    :postcode "DE14 1LS",
    :id :east-staffordshire-borough}
   {:common-name "East Suffolk Council",
    :address-1 "East Suffolk House",
    :address-2 "Station Road",
    :town "Melton",
    :county "Suffolk",
    :postcode "IP12 1RT",
    :id :east-suffolk}
   {:common-name "East Sussex County Council",
    :address-1 "County Hall",
    :address-2 "St Anne's Crescent",
    :town "Lewes",
    :county "East Sussex",
    :postcode "BN7 1UE",
    :id :east-sussex-county}
   {:common-name "Eastbourne Borough Council",
    :address-1 "1 Grove Road",
    :address-2 "",
    :town "Eastbourne",
    :county "East Sussex",
    :postcode "BN21 4TW",
    :id :eastbourne-borough}
   {:common-name "Eastleigh Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Leigh Road",
    :town "Eastleigh",
    :county "Hampshire",
    :postcode "SO50 9YN",
    :id :eastleigh-borough}
   {:common-name "Eden District Council",
    :address-1 "Town Hall",
    :address-2 "Penrith",
    :town "Cumbria",
    :county "",
    :postcode "CA11 7QF",
    :id :eden-district}
   {:common-name "Elmbridge Borough Council",
    :address-1 "Civic Centre",
    :address-2 "High Street",
    :town "Esher",
    :county "Surrey",
    :postcode "KT10 9SD",
    :id :elmbridge-borough}
   {:common-name "Epping Forest District Council",
    :address-1 "Civic Offices",
    :address-2 "High Street",
    :town "Epping",
    :county "Essex",
    :postcode "CM16 4BZ",
    :id :epping-forest-district}
   {:common-name "Epsom and Ewell Borough Council",
    :address-1 "Town Hall",
    :address-2 "The Parade",
    :town "Epsom",
    :county "Surrey",
    :postcode "KT18 5BY",
    :id :epsom-and-ewell-borough}
   {:common-name "Erewash Borough Council",
    :address-1 "Town Hall",
    :address-2 "Ilkeston",
    :town "Derbyshire",
    :county "Derbyshire",
    :postcode "DE7 5RP",
    :id :erewash-borough}
   {:common-name "Essex County Council",
    :address-1 "County Hall",
    :address-2 "Market Road",
    :town "Chelmsford",
    :county "Essex",
    :postcode "CM1 1QH",
    :id :essex-county}
   {:common-name "Exeter City Council",
    :address-1 "Civic Centre",
    :address-2 "Paris Street",
    :town "Exeter",
    :county "Devon",
    :postcode "EX1 1JN",
    :id :exeter-city}
   {:common-name "Fareham Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Civic Way",
    :town "Fareham",
    :county "Hampshire",
    :postcode "PO16 7AZ",
    :id :fareham-borough}
   {:common-name "Fenland District Council",
    :address-1 "Fenland Hall",
    :address-2 "County Road",
    :town "March",
    :county "Cambridgeshire",
    :postcode "PE15 8NQ",
    :id :fenland-district}
   {:common-name "Flintshire County Council",
    :address-1 "County Hall",
    :address-2 "The Nant",
    :town "Mold",
    :county "Flintshire",
    :postcode "CH7 6NB",
    :id :flintshire-county}
   {:common-name "Forest Heath District Council",
    :address-1 "District Offices",
    :address-2 "College Heath Road",
    :town "Mildenhall",
    :county "Suffolk",
    :postcode "IP28 7EY",
    :id :forest-heath-district}
   {:common-name "Forest of Dean District Council",
    :address-1 "Council Offices",
    :address-2 "High Street",
    :town "Coleford",
    :county "Gloucestershire",
    :postcode "GL16 8HG",
    :id :forest-of-dean-district}
   {:common-name "Fylde Borough Council",
    :address-1 "Town Hall",
    :address-2 "St. Annes Road West",
    :town "Lytham St Annes",
    :county "Lancashire",
    :postcode "FY8 1LW",
    :id :fylde-borough}
   {:common-name "Gateshead Metropolitan Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Regent Street",
    :town "Gateshead",
    :county "Tyne and Wear",
    :postcode "NE8 1HH",
    :id :gateshead-metropolitan-borough}
   {:common-name "Gedling Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Arnold",
    :town "Nottingham",
    :county "Nottinghamshire",
    :postcode "NG5 6LU",
    :id :gedling-borough}
   {:common-name "Gloucester City Council",
    :address-1 "Herbert Warehouse",
    :address-2 "The Docks",
    :town "Gloucester",
    :county "Gloucestershire",
    :postcode "GL1 2EQ",
    :id :gloucester-city}
   {:common-name "Gloucestershire County Council",
    :address-1 "Shire Hall",
    :address-2 "Westgate Street",
    :town "Gloucester",
    :county "Gloucestershire",
    :postcode "GL1 2TG",
    :id :gloucestershire-county}
   {:common-name "Gosport Borough Council",
    :address-1 "Town Hall",
    :address-2 "High Street",
    :town "Gosport",
    :county "Hampshire",
    :postcode "PO12 1EB",
    :id :gosport-borough}
   {:common-name "Gravesham Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Windmill Street",
    :town "Gravesend",
    :county "Kent",
    :postcode "DA12 1AU",
    :id :gravesham-borough}
   {:common-name "Great Yarmouth Borough Council",
    :address-1 "Town Hall",
    :address-2 "Hall Plain",
    :town "Great Yarmouth",
    :county "Norfolk",
    :postcode "NR30 2QF",
    :id :great-yarmouth-borough}
   {:common-name "Guildford Borough Council",
    :address-1 "Millmead House",
    :address-2 "Millmead",
    :town "Guildford",
    :county "Surrey",
    :postcode "GU2 4BB",
    :id :guildford-borough}
   {:common-name "Gwynedd County Council",
    :address-1 "County Offices",
    :address-2 "Caernarfon",
    :town "Gwynedd",
    :county "North Wales",
    :postcode "LL55 1SH",
    :id :gwynedd-county}
   {:common-name "Halton Borough Council",
    :address-1 "Municipal Building",
    :address-2 "Kingsway",
    :town "Widnes",
    :county "Cheshire",
    :postcode "WA8 7QF",
    :id :halton-borough}
   {:common-name "Hambleton District Council",
    :address-1 "Civic Centre",
    :address-2 "Stone Cross",
    :town "Northallerton",
    :county "North Yorkshire",
    :postcode "DL6 2UU",
    :id :hambleton-district}
   {:common-name "Hampshire County Council",
    :address-1 "The Castle",
    :address-2 "Winchester",
    :town "Hampshire",
    :county "Hampshire",
    :postcode "SO23 8UJ",
    :id :hampshire-county}
   {:common-name "Harborough District Council",
    :address-1 "The Symington Building",
    :address-2 "Adam and Eve Street",
    :town "Market Harborough",
    :county "Leicestershire",
    :postcode "LE16 7AG",
    :id :harborough-district}
   {:common-name "Harlow Council",
    :address-1 "Civic Centre",
    :address-2 "The Water Gardens",
    :town "Harlow",
    :county "Essex",
    :postcode "CM20 1WG",
    :id :harlow}
   {:common-name "Harrogate Borough Council",
    :address-1 "Civic Centre",
    :address-2 "St Lukes Avenue",
    :town "Harrogate",
    :county "North Yorkshire",
    :postcode "HG1 2AE",
    :id :harrogate-borough}
   {:common-name "Hart District Council",
    :address-1 "Harlington Way",
    :address-2 "Fleet",
    :town "Hampshire",
    :county "Hampshire",
    :postcode "GU51 4AE",
    :id :hart-district}
   {:common-name "Hartlepool Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Victoria Road",
    :town "Hartlepool",
    :county "County Durham",
    :postcode "TS24 8AY",
    :id :hartlepool-borough}
   {:common-name "Hastings Borough Council",
    :address-1 "Muriel Matters House",
    :address-2 "Breeds Place",
    :town "Hastings",
    :county "East Sussex",
    :postcode "TN34 3UY",
    :id :hastings-borough}
   {:common-name "Havant Borough Council",
    :address-1 "Public Service Plaza",
    :address-2 "Civic Centre Road",
    :town "Havant",
    :county "Hampshire",
    :postcode "PO9 2AX",
    :id :havant-borough}
   {:common-name "Herefordshire Council",
    :address-1 "Plough Lane",
    :address-2 "Hereford",
    :town "Herefordshire",
    :county "Herefordshire",
    :postcode "HR4 0LE",
    :id :herefordshire}
   {:common-name "Hertfordshire County Council",
    :address-1 "County Hall",
    :address-2 "Pegs Lane",
    :town "Hertford",
    :county "Hertfordshire",
    :postcode "SG13 8DQ",
    :id :hertfordshire-county}
   {:common-name "Hertsmere Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Elstree Way",
    :town "Borehamwood",
    :county "Hertfordshire",
    :postcode "WD6 1WA",
    :id :hertsmere-borough}
   {:common-name "High Peak Borough Council",
    :address-1 "Town Hall",
    :address-2 "Market Place",
    :town "Buxton",
    :county "Derbyshire",
    :postcode "SK17 6EL",
    :id :high-peak-borough}
   {:common-name "Hinckley and Bosworth Borough Council",
    :address-1 "Hinckley Hub",
    :address-2 "Rugby Road",
    :town "Hinckley",
    :county "Leicestershire",
    :postcode "LE10 0FR",
    :id :hinckley-and-bosworth-borough}
   {:common-name "Horsham District Council",
    :address-1 "Parkside",
    :address-2 "Chart Way",
    :town "Horsham",
    :county "West Sussex",
    :postcode "RH12 1RL",
    :id :horsham-district}
   {:common-name "Huntingdonshire District Council",
    :address-1 "Pathfinder House",
    :address-2 "St Mary's Street",
    :town "Huntingdon",
    :county "Cambridgeshire",
    :postcode "PE29 3TN",
    :id :huntingdonshire-district}
   {:common-name "Hyndburn Borough Council",
    :address-1 "Scaitcliffe House",
    :address-2 "Ormerod Street",
    :town "Accrington",
    :county "Lancashire",
    :postcode "BB5 0PF",
    :id :hyndburn-borough}
   {:common-name "Ipswich Borough Council",
    :address-1 "Grafton House",
    :address-2 "15-17 Russell Road",
    :town "Ipswich",
    :county "Suffolk",
    :postcode "IP1 2DE",
    :id :ipswich-borough}
   {:common-name "Isle of Anglesey County Council",
    :address-1 "Council Offices",
    :address-2 "Llangefni",
    :town "Isle of Anglesey",
    :county "Anglesey",
    :postcode "LL77 7TW",
    :id :isle-of-anglesey-county}
   {:common-name "Isle of Wight Council",
    :address-1 "County Hall",
    :address-2 "High Street",
    :town "Newport",
    :county "Isle of Wight",
    :postcode "PO30 1UD",
    :id :isle-of-wight}
   {:common-name "Isles of Scilly Council",
    :address-1 "Town Hall",
    :address-2 "The Parade",
    :town "St Mary's",
    :county "Isles of Scilly",
    :postcode "TR21 0LW",
    :id :isles-of-scilly}
   {:common-name "Kent County Council",
    :address-1 "County Hall",
    :address-2 "Maidstone",
    :town "Kent",
    :county "Kent",
    :postcode "ME14 1XQ",
    :id :kent-county}
   {:common-name "Kettering Borough Council",
    :address-1 "Municipal Offices",
    :address-2 "Bowling Green Road",
    :town "Kettering",
    :county "Northamptonshire",
    :postcode "NN15 7QX",
    :id :kettering-borough}
   {:common-name "King's Lynn and West Norfolk Borough Council",
    :address-1 "King's Court",
    :address-2 "Chapel Street",
    :town "King's Lynn",
    :county "Norfolk",
    :postcode "PE30 1EX",
    :id :kings-lynn-and-west-norfolk-borough}
   {:common-name "Kingston-upon-Hull City Council",
    :address-1 "Guildhall",
    :address-2 "Alfred Gelder Street",
    :town "Kingston-upon-Hull",
    :county "East Yorkshire",
    :postcode "HU1 2AA",
    :id :kingstonuponhull-city}
   {:common-name "Kirklees Council",
    :address-1 "Civic Centre 3",
    :address-2 "Market Street",
    :town "Huddersfield",
    :county "West Yorkshire",
    :postcode "HD1 2YZ",
    :id :kirklees}
   {:common-name "Knowsley Metropolitan Borough Council",
    :address-1 "Municipal Building",
    :address-2 "Archway Road",
    :town "Huyton",
    :county "Merseyside",
    :postcode "L36 9UX",
    :id :knowsley-metropolitan-borough}
   {:common-name "Lancashire County Council",
    :address-1 "County Hall",
    :address-2 "Fishergate",
    :town "Preston",
    :county "Lancashire",
    :postcode "PR1 8XJ",
    :id :lancashire-county}
   {:common-name "Lancaster City Council",
    :address-1 "Town Hall",
    :address-2 "Dalton Square",
    :town "Lancaster",
    :county "Lancashire",
    :postcode "LA1 1PJ",
    :id :lancaster-city}
   {:common-name "Leeds City Council",
    :address-1 "Civic Hall",
    :address-2 "Calverley Street",
    :town "Leeds",
    :county "West Yorkshire",
    :postcode "LS1 1UR",
    :id :leeds-city}
   {:common-name "Leicester City Council",
    :address-1 "City Hall",
    :address-2 "115 Charles Street",
    :town "Leicester",
    :county "Leicestershire",
    :postcode "LE1 1FZ",
    :id :leicester-city}
   {:common-name "Leicestershire County Council",
    :address-1 "County Hall",
    :address-2 "Glenfield",
    :town "Leicester",
    :county "Leicestershire",
    :postcode "LE3 8RA",
    :id :leicestershire-county}
   {:common-name "Lewes District Council",
    :address-1 "Southover House",
    :address-2 "Southover Road",
    :town "Lewes",
    :county "East Sussex",
    :postcode "BN7 1AB",
    :id :lewes-district}
   {:common-name "Lichfield District Council",
    :address-1 "District Council House",
    :address-2 "Frog Lane",
    :town "Lichfield",
    :county "Staffordshire",
    :postcode "WS13 6YY",
    :id :lichfield-district}
   {:common-name "Lincolnshire County Council",
    :address-1 "County Offices",
    :address-2 "Newland",
    :town "Lincoln",
    :county "Lincolnshire",
    :postcode "LN1 1YL",
    :id :lincolnshire-county}
   {:common-name "Liverpool City Council",
    :address-1 "Municipal Buildings",
    :address-2 "Dale Street",
    :town "Liverpool",
    :county "Merseyside",
    :postcode "L2 2DH",
    :id :liverpool-city}
   {:common-name "London Borough of Barking and Dagenham",
    :address-1 "Town Hall",
    :address-2 "1 Town Square",
    :town "Barking",
    :county "Essex",
    :postcode "IG11 7LU",
    :id :london-borough-of-barking-and-dagenham}
   {:common-name "London Borough of Barnet",
    :address-1 "North London Business Park",
    :address-2 "Oakleigh Road South",
    :town "London",
    :postcode "N11 1NP",
    :id :london-borough-of-barnet}
   {:common-name "London Borough of Bexley",
    :address-1 "Civic Offices",
    :address-2 "2 Watling Street",
    :town "Bexleyheath",
    :county "Kent",
    :postcode "DA6 7AT",
    :id :london-borough-of-bexley}
   {:common-name "London Borough of Brent",
    :address-1 "Brent Civic Centre",
    :address-2 "Engineers Way",
    :town "Wembley",
    :county "Middlesex",
    :postcode "HA9 0FJ",
    :id :london-borough-of-brent}
   {:common-name "London Borough of Bromley",
    :address-1 "Civic Centre",
    :address-2 "Stockwell Close",
    :town "Bromley",
    :county "Kent",
    :postcode "BR1 3UH",
    :id :london-borough-of-bromley}
   {:common-name "London Borough of Camden",
    :address-1 "5 Pancras Square",
    :address-2 "London",
    :postcode "N1C 4AG",
    :id :london-borough-of-camden}
   {:common-name "London Borough of Croydon",
    :address-1 "Bernard Weatherill House",
    :address-2 "8 Mint Walk",
    :town "Croydon",
    :county "Surrey",
    :postcode "CR0 1EA",
    :id :london-borough-of-croydon}
   {:common-name "London Borough of Ealing",
    :address-1 "Perceval House",
    :address-2 "14-16 Uxbridge Road",
    :town "Ealing",
    :county "London",
    :postcode "W5 2HL",
    :id :london-borough-of-ealing}
   {:common-name "London Borough of Enfield",
    :address-1 "Civic Centre",
    :address-2 "Silver Street",
    :town "Enfield",
    :county "Middlesex",
    :postcode "EN1 3XY",
    :id :london-borough-of-enfield}
   {:common-name "London Borough of Hackney",
    :address-1 "Hackney Town Hall",
    :address-2 "Mare Street",
    :town "London",
    :postcode "E8 1EA",
    :id :london-borough-of-hackney}
   {:common-name "London Borough of Hammersmith & Fulham",
    :address-1 "Town Hall",
    :address-2 "King Street",
    :town "London",
    :postcode "W6 9JU",
    :id :london-borough-of-hammersmith--fulham}
   {:common-name "London Borough of Haringey",
    :address-1 "Civic Centre",
    :address-2 "High Road",
    :town "London",
    :postcode "N22 8LE",
    :id :london-borough-of-haringey}
   {:common-name "London Borough of Harrow",
    :address-1 "Civic Centre",
    :address-2 "Station Road",
    :town "Harrow",
    :county "Middlesex",
    :postcode "HA1 2XY",
    :id :london-borough-of-harrow}
   {:common-name "London Borough of Havering",
    :address-1 "Town Hall",
    :address-2 "Main Road",
    :town "Romford",
    :county "Essex",
    :postcode "RM1 3BB",
    :id :london-borough-of-havering}
   {:common-name "London Borough of Hillingdon",
    :address-1 "Civic Centre",
    :address-2 "High Street",
    :town "Uxbridge",
    :county "Middlesex",
    :postcode "UB8 1UW",
    :id :london-borough-of-hillingdon}
   {:common-name "London Borough of Hounslow",
    :address-1 "Hounslow House",
    :address-2 "7 Bath Road",
    :town "Hounslow",
    :county "London",
    :postcode "TW3 3EB",
    :id :london-borough-of-hounslow}
   {:common-name "London Borough of Islington",
    :address-1 "Islington Town Hall",
    :address-2 "Upper Street",
    :town "London",
    :county "London",
    :postcode "N1 2UD",
    :id :london-borough-of-islington}
   {:common-name "London Borough of Lambeth",
    :address-1 "Lambeth Town Hall",
    :address-2 "Brixton Hill",
    :town "London",
    :county "London",
    :postcode "SW2 1RW",
    :id :london-borough-of-lambeth}
   {:common-name "London Borough of Lewisham",
    :address-1 "Lewisham Town Hall",
    :address-2 "Catford Road",
    :town "London",
    :county "London",
    :postcode "SE6 4RU",
    :id :london-borough-of-lewisham}
   {:common-name "London Borough of Merton",
    :address-1 "Civic Centre",
    :address-2 "London Road",
    :town "Morden",
    :county "London",
    :postcode "SM4 5DX",
    :id :london-borough-of-merton}
   {:common-name "London Borough of Newham",
    :address-1 "Newham Dockside",
    :address-2 "1000 Dockside Road",
    :town "London",
    :county "London",
    :postcode "E16 2QU",
    :id :london-borough-of-newham}
   {:common-name "London Borough of Redbridge",
    :address-1 "Redbridge Town Hall",
    :address-2 "128-142 High Road",
    :town "Ilford",
    :county "London",
    :postcode "IG1 1DD",
    :id :london-borough-of-redbridge}
   {:common-name "London Borough of Richmond upon Thames",
    :address-1 "Civic Centre",
    :address-2 "44 York Street",
    :town "Twickenham",
    :county "London",
    :postcode "TW1 3BZ",
    :id :london-borough-of-richmond-upon-thames}
   {:common-name "London Borough of Southwark",
    :address-1 "160 Tooley Street",
    :address-2 "London",
    :county "London",
    :postcode "SE1 2QH",
    :id :london-borough-of-southwark}
   {:common-name "London Borough of Sutton",
    :address-1 "Civic Offices",
    :address-2 "St Nicholas Way",
    :town "Sutton",
    :county "London",
    :postcode "SM1 1EA",
    :id :london-borough-of-sutton}
   {:common-name "London Borough of Tower Hamlets",
    :address-1 "Town Hall",
    :address-2 "Mulberry Place",
    :town "5 Clove Crescent",
    :county "London",
    :postcode "E14 2BG",
    :id :london-borough-of-tower-hamlets}
   {:common-name "London Borough of Waltham Forest",
    :address-1 "Waltham Forest Town Hall",
    :address-2 "Forest Road",
    :town "Walthamstow",
    :county "London",
    :postcode "E17 4JF",
    :id :london-borough-of-waltham-forest}
   {:common-name "London Borough of Wandsworth",
    :address-1 "The Town Hall",
    :address-2 "Wandsworth High Street",
    :town "London",
    :county "London",
    :postcode "SW18 2PU",
    :id :london-borough-of-wandsworth}
   {:common-name "Luton Borough Council",
    :address-1 "Town Hall",
    :address-2 "George Street",
    :town "Luton",
    :county "Bedfordshire",
    :postcode "LU1 2BQ",
    :id :luton-borough}
   {:common-name "Maidstone Borough Council",
    :address-1 "Maidstone House",
    :address-2 "King Street",
    :town "Maidstone",
    :county "Kent",
    :postcode "ME15 6JQ",
    :id :maidstone-borough}
   {:common-name "Maldon District Council",
    :address-1 "Princes Road",
    :address-2 "Maldon",
    :county "Essex",
    :postcode "CM9 5DL",
    :id :maldon-district}
   {:common-name "Malvern Hills District Council",
    :address-1 "Council House",
    :address-2 "Avenue Road",
    :town "Malvern",
    :county "Worcestershire",
    :postcode "WR14 3AF",
    :id :malvern-hills-district}
   {:common-name "Manchester City Council",
    :address-1 "Town Hall",
    :address-2 "Albert Square",
    :town "Manchester",
    :county "Greater Manchester",
    :postcode "M60 2LA",
    :id :manchester-city}
   {:common-name "Mansfield District Council",
    :address-1 "Civic Centre",
    :address-2 "Chesterfield Road South",
    :town "Mansfield",
    :county "Nottinghamshire",
    :postcode "NG19 7BH",
    :id :mansfield-district}
   {:common-name "Medway Council",
    :address-1 "Gun Wharf",
    :address-2 "Dock Road",
    :town "Chatham",
    :county "Kent",
    :postcode "ME4 4TR",
    :id :medway}
   {:common-name "Melton Borough Council",
    :address-1 "Parkside",
    :address-2 "Station Approach",
    :town "Melton Mowbray",
    :county "Leicestershire",
    :postcode "LE13 1GH",
    :id :melton-borough}
   {:common-name "Mendip District Council",
    :address-1 "Cannards Grave Road",
    :address-2 "Shepton Mallet",
    :county "Somerset",
    :postcode "BA4 5BT",
    :id :mendip-district}
   {:common-name "Merthyr Tydfil County Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Castle Street",
    :town "Merthyr Tydfil",
    :county "Mid Glamorgan",
    :postcode "CF47 8AN",
    :id :merthyr-tydfil-county-borough}
   {:common-name "Mid Devon District Council",
    :address-1 "Phoenix House",
    :address-2 "Phoenix Lane",
    :town "Tiverton",
    :county "Devon",
    :postcode "EX16 6PP",
    :id :mid-devon-district}
   {:common-name "Mid Suffolk District Council",
    :address-1 "131 High Street",
    :address-2 "Needham Market",
    :town "Ipswich",
    :county "Suffolk",
    :postcode "IP6 8DL",
    :id :mid-suffolk-district}
   {:common-name "Mid Sussex District Council",
    :address-1 "Oaklands",
    :address-2 "Oaklands Road",
    :town "Haywards Heath",
    :county "West Sussex",
    :postcode "RH16 1SS",
    :id :mid-sussex-district}
   {:common-name "Middlesbrough Borough Council",
    :address-1 "Town Hall",
    :address-2 "Albert Road",
    :town "Middlesbrough",
    :county "North Yorkshire",
    :postcode "TS1 2PA",
    :id :middlesbrough-borough}
   {:common-name "Milton Keynes Council",
    :address-1 "Civic Offices",
    :address-2 "1 Saxon Gate East",
    :town "Milton Keynes",
    :county "Buckinghamshire",
    :postcode "MK9 3EJ",
    :id :milton-keynes}
   {:common-name "Mole Valley District Council",
    :address-1 "Pippbrook",
    :address-2 "Dorking",
    :town "Surrey",
    :county "Surrey",
    :postcode "RH4 1SJ",
    :id :mole-valley-district}
   {:common-name "Monmouthshire County Council",
    :address-1 "County Hall",
    :address-2 "Rhadyr",
    :town "Usk",
    :county "Monmouthshire",
    :postcode "NP15 1GA",
    :id :monmouthshire-county}
   {:common-name "Neath Port Talbot County Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Port Talbot",
    :town "West Glamorgan",
    :county "West Glamorgan",
    :postcode "SA13 1PJ",
    :id :neath-port-talbot-county-borough}
   {:common-name "New Forest District Council",
    :address-1 "Appletree Court",
    :address-2 "Beaulieu Road",
    :town "Lyndhurst",
    :county "Hampshire",
    :postcode "SO43 7PA",
    :id :new-forest-district}
   {:common-name "Newark and Sherwood District Council",
    :address-1 "Castle House",
    :address-2 "Great North Road",
    :town "Newark",
    :county "Nottinghamshire",
    :postcode "NG24 1BY",
    :id :newark-and-sherwood-district}
   {:common-name "Newcastle-Under-Lyme District Council",
    :address-1 "Civic Offices",
    :address-2 "Merrial Street",
    :town "Newcastle-Under-Lyme",
    :county "Staffordshire",
    :postcode "ST5 2AG",
    :id :newcastleunderlyme-district}
   {:common-name "Newport City Council",
    :address-1 "Civic Centre",
    :address-2 "Godfrey Road",
    :town "Newport",
    :county "Gwent",
    :postcode "NP20 4UR",
    :id :newport-city}
   {:common-name "Newcastle-upon-Tyne City Council",
    :address-1 "Civic Centre",
    :address-2 "Barras Bridge",
    :town "Newcastle-upon-Tyne",
    :county "Tyne and Wear",
    :postcode "NE1 8QH",
    :id :newcastleupontyne-city}
   {:common-name "Norfolk County Council",
    :address-1 "County Hall",
    :address-2 "Martineau Lane",
    :town "Norwich",
    :county "Norfolk",
    :postcode "NR1 2DH",
    :id :norfolk-county}
   {:common-name "North Devon Council",
    :address-1 "Brynsworthy Environment Centre",
    :address-2 "Roundswell Business Park",
    :town "Barnstaple",
    :county "Devon",
    :postcode "EX31 3NP",
    :id :north-devon}
   {:common-name "North Dorset District Council",
    :address-1 "Norden",
    :address-2 "Salisbury Road",
    :town "Blandford Forum",
    :county "Dorset",
    :postcode "DT11 9LL",
    :id :north-dorset-district}
   {:common-name "North East Derbyshire District Council",
    :address-1 "2013 Mill Lane",
    :address-2 "Wingerworth",
    :town "Chesterfield",
    :county "Derbyshire",
    :postcode "S42 6NG",
    :id :north-east-derbyshire-district}
   {:common-name "North East Lincolnshire Council",
    :address-1 "Municipal Offices",
    :address-2 "Town Hall Square",
    :town "Grimsby",
    :county "North East Lincolnshire",
    :postcode "DN31 1HU",
    :id :north-east-lincolnshire}
   {:common-name "North Hertfordshire District Council",
    :address-1 "Council Offices",
    :address-2 "Gernon Road",
    :town "Letchworth Garden City",
    :county "Hertfordshire",
    :postcode "SG6 3JF",
    :id :north-hertfordshire-district}
   {:common-name "North Kesteven District Council",
    :address-1 "Kesteven Street",
    :town "Sleaford",
    :county "Lincolnshire",
    :postcode "NG34 7EF",
    :id :north-kesteven-district}
   {:common-name "North Lincolnshire Council",
    :address-1 "Church Square House",
    :address-2 "30-40 High Street",
    :town "Scunthorpe",
    :county "Lincolnshire",
    :postcode "DN15 6NL",
    :id :north-lincolnshire}
   {:common-name "North Norfolk District Council",
    :address-1 "Council Offices",
    :address-2 "Holt Road",
    :town "Cromer",
    :county "Norfolk",
    :postcode "NR27 9EN",
    :id :north-norfolk-district}
   {:common-name "North Somerset Council",
    :address-1 "Town Hall",
    :address-2 "Walliscote Grove Road",
    :town "Weston-super-Mare",
    :county "Somerset",
    :postcode "BS23 1UJ",
    :id :north-somerset}
   {:common-name "North Tyneside Metropolitan Borough Council",
    :address-1 "Quadrant West",
    :address-2 "Silverlink North",
    :town "Cobalt Business Park",
    :county "Tyne and Wear",
    :postcode "NE27 0BY",
    :id :north-tyneside-metropolitan-borough}
   {:common-name "North Warwickshire Borough Council",
    :address-1 "The Council House",
    :address-2 "South Street",
    :town "Atherstone",
    :county "Warwickshire",
    :postcode "CV9 1DE",
    :id :north-warwickshire-borough}
   {:common-name "North West Leicestershire District Council",
    :address-1 "Council Offices",
    :address-2 "Whitwick Road",
    :town "Coalville",
    :county "Leicestershire",
    :postcode "LE67 3FJ",
    :id :north-west-leicestershire-district}
   {:common-name "North Yorkshire County Council",
    :address-1 "County Hall",
    :address-2 "Racecourse Lane",
    :town "Northallerton",
    :county "North Yorkshire",
    :postcode "DL7 8AD",
    :id :north-yorkshire-county}
   {:common-name "Northampton Borough Council",
    :address-1 "The Guildhall",
    :address-2 "St Giles' Square",
    :town "Northampton",
    :county "Northamptonshire",
    :postcode "NN1 1DE",
    :id :northampton-borough}
   {:common-name "Northamptonshire County Council",
    :address-1 "One Angel Square",
    :address-2 "Angel Street",
    :town "Northampton",
    :county "Northamptonshire",
    :postcode "NN1 1ED",
    :id :northamptonshire-county}
   {:common-name "Northumberland County Council",
    :address-1 "County Hall",
    :address-2 "Morpeth",
    :county "Northumberland",
    :postcode "NE61 2EF",
    :id :northumberland-county}
   {:common-name "Norwich City Council",
    :address-1 "City Hall",
    :address-2 "St. Peter's Street",
    :town "Norwich",
    :postcode "NR2 1NH",
    :county "Norfolk",
    :id :norwich-city}
   {:common-name "Nottingham City Council",
    :address-1 "Loxley House",
    :address-2 "Station Street",
    :town "Nottingham",
    :postcode "NG2 3NG",
    :county "Nottinghamshire",
    :id :nottingham-city}
   {:common-name "Nottinghamshire County Council",
    :address-1 "County Hall",
    :address-2 "West Bridgford",
    :town "Nottingham",
    :postcode "NG2 7QP",
    :county "Nottinghamshire",
    :id :nottinghamshire-county}
   {:common-name "Nuneaton and Bedworth Borough Council",
    :address-1 "Town Hall",
    :address-2 "Coton Road",
    :town "Nuneaton",
    :postcode "CV11 5AA",
    :county "Warwickshire",
    :id :nuneaton-and-bedworth-borough}
   {:common-name "Oadby and Wigston District Council",
    :address-1 "Council Offices",
    :address-2 "Station Road",
    :town "Wigston",
    :postcode "LE18 2DR",
    :county "Leicestershire",
    :id :oadby-and-wigston-district}
   {:common-name "Oldham Metropolitan Borough Council",
    :address-1 "Civic Centre",
    :address-2 "West Street",
    :town "Oldham",
    :postcode "OL1 1UG",
    :county "Greater Manchester",
    :id :oldham-metropolitan-borough}
   {:common-name "Oxford City Council",
    :address-1 "St Aldate's Chambers",
    :address-2 "St Aldate's",
    :town "Oxford",
    :postcode "OX1 1DS",
    :county "Oxfordshire",
    :id :oxford-city}
   {:common-name "Oxfordshire County Council",
    :address-1 "County Hall",
    :address-2 "New Road",
    :town "Oxford",
    :postcode "OX1 1ND",
    :county "Oxfordshire",
    :id :oxfordshire-county}
   {:common-name "Pembrokeshire County Council",
    :address-1 "County Hall",
    :address-2 "Haverfordwest",
    :town "Pembrokeshire",
    :postcode "SA61 1TP",
    :county "Pembrokeshire",
    :id :pembrokeshire-county}
   {:common-name "Pendle Borough Council",
    :address-1 "Number One Market Street",
    :address-2 "Nelson",
    :town "Lancashire",
    :postcode "BB9 7LJ",
    :county "Lancashire",
    :id :pendle-borough}
   {:common-name "Perth and Kinross Council",
    :address-1 "2 High Street",
    :address-2 "Perth",
    :town "Perth and Kinross",
    :postcode "PH1 5PH",
    :county "Perth and Kinross",
    :id :perth-and-kinross}
   {:common-name "Peterborough City Council",
    :address-1 "Town Hall",
    :address-2 "Bridge Street",
    :town "Peterborough",
    :postcode "PE1 1HF",
    :county "Cambridgeshire",
    :id :peterborough-city}
   {:common-name "Plymouth City Council",
    :address-1 "Civic Centre",
    :address-2 "Plymouth",
    :town "Devon",
    :postcode "PL1 2AA",
    :county "Devon",
    :id :plymouth-city}
   {:common-name "Portsmouth City Council",
    :address-1 "Civic Offices",
    :address-2 "Guildhall Square",
    :town "Portsmouth",
    :postcode "PO1 2AL",
    :county "Hampshire",
    :id :portsmouth-city}
   {:common-name "Powys County Council",
    :address-1 "County Hall",
    :address-2 "Spa Road East",
    :town "Llandrindod Wells",
    :county "Powys",
    :postcode "LD1 5LG",
    :id :powys-county}
   {:common-name "Preston City Council",
    :address-1 "Town Hall",
    :address-2 "Lancaster Rd",
    :town "Preston",
    :county "Lancashire",
    :postcode "PR1 2RL",
    :id :preston-city}
   {:common-name "Purbeck District Council",
    :address-1 "Westport House",
    :address-2 "Worgret Road",
    :town "Wareham",
    :county "Dorset",
    :postcode "BH20 4PP",
    :id :purbeck-district}
   {:common-name "Reading Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Bridge Street",
    :town "Reading",
    :county "Berkshire",
    :postcode "RG1 2LU",
    :id :reading-borough}
   {:common-name "Redcar and Cleveland Council",
    :address-1 "Redcar & Cleveland House",
    :address-2 "Kirkleatham Street",
    :town "Redcar",
    :county "North Yorkshire",
    :postcode "TS10 1RT",
    :id :redcar-and-cleveland}
   {:common-name "Redditch Borough Council",
    :address-1 "Town Hall",
    :address-2 "Walter Stranz Square",
    :town "Redditch",
    :county "Worcestershire",
    :postcode "B98 8AH",
    :id :redditch-borough}
   {:common-name "Reigate & Banstead Borough Council",
    :address-1 "Town Hall",
    :address-2 "Castlefield Road",
    :town "Reigate",
    :county "Surrey",
    :postcode "RH2 0SH",
    :id :reigate--banstead-borough}
   {:common-name "Rhondda Cynon Taf County Borough Council",
    :address-1 "The Pavilions",
    :address-2 "Cambrian Park",
    :town "Clydach Vale",
    :county "Rhondda Cynon Taf",
    :postcode "CF40 2XX",
    :id :rhondda-cynon-taf-county-borough}
   {:common-name "Ribble Valley Borough Council",
    :address-1 "Council Offices",
    :address-2 "Church Walk",
    :town "Clitheroe",
    :county "Lancashire",
    :postcode "BB7 2RA",
    :id :ribble-valley-borough}
   {:common-name "Richmondshire District Council",
    :address-1 "Mercury House",
    :address-2 "Station Road",
    :town "Richmond",
    :county "North Yorkshire",
    :postcode "DL10 4JX",
    :id :richmondshire-district}
   {:common-name "Rochdale Metropolitan Borough Council",
    :address-1 "Number One Riverside",
    :address-2 "Smith Street",
    :town "Rochdale",
    :county "Greater Manchester",
    :postcode "OL16 1XU",
    :id :rochdale-metropolitan-borough}
   {:common-name "Rochford District Council",
    :address-1 "South Street",
    :town "Rochford",
    :county "Essex",
    :postcode "SS4 1BW",
    :id :rochford-district}
   {:common-name "Rossendale Borough Council",
    :address-1 "Futures Park",
    :address-2 "Bacup",
    :town "Lancashire",
    :postcode "OL13 0BB",
    :county "Lancashire",
    :id :rossendale-borough}
   {:common-name "Rother District Council",
    :address-1 "Town Hall",
    :address-2 "London Road",
    :town "Bexhill-on-Sea",
    :county "East Sussex",
    :postcode "TN39 3JX",
    :id :rother-district}
   {:common-name "Rotherham Metropolitan Borough Council",
    :address-1 "Riverside House",
    :address-2 "Main Street",
    :town "Rotherham",
    :county "South Yorkshire",
    :postcode "S60 1AE",
    :id :rotherham-metropolitan-borough}
   {:common-name "Royal Borough of Greenwich",
    :address-1 "The Woolwich Centre",
    :address-2 "35 Wellington Street",
    :town "London",
    :postcode "SE18 6HQ",
    :id :royal-borough-of-greenwich}
   {:common-name "Royal Borough of Kensington and Chelsea",
    :address-1 "The Town Hall",
    :address-2 "Hornton Street",
    :town "London",
    :postcode "W8 7NX",
    :id :royal-borough-of-kensington-and-chelsea}
   {:common-name "Royal Borough of Kingston upon Thames",
    :address-1 "Guildhall",
    :address-2 "High Street",
    :town "Kingston upon Thames",
    :county "Surrey",
    :postcode "KT1 1EU",
    :id :royal-borough-of-kingston-upon-thames}
   {:common-name "Royal Borough of Windsor and Maidenhead",
    :address-1 "Town Hall",
    :address-2 "St Ives Road",
    :town "Maidenhead",
    :county "Berkshire",
    :postcode "SL6 1RF",
    :id :royal-borough-of-windsor-and-maidenhead}
   {:common-name "Rugby Borough Council",
    :address-1 "Town Hall",
    :address-2 "Evreux Way",
    :town "Rugby",
    :county "Warwickshire",
    :postcode "CV21 2RR",
    :id :rugby-borough}
   {:common-name "Runnymede Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Station Road",
    :town "Addlestone",
    :county "Surrey",
    :postcode "KT15 2AH",
    :id :runnymede-borough}
   {:common-name "Rushcliffe Borough Council",
    :address-1 "Rushcliffe Arena",
    :address-2 "Rugby Road",
    :town "West Bridgford",
    :county "Nottinghamshire",
    :postcode "NG2 7YG",
    :id :rushcliffe-borough}
   {:common-name "Rushmoor Borough Council",
    :address-1 "Council Offices",
    :address-2 "Farnborough Road",
    :town "Farnborough",
    :county "Hampshire",
    :postcode "GU14 7JU",
    :id :rushmoor-borough}
   {:common-name "Rutland County Council",
    :address-1 "Catmose",
    :town "Oakham",
    :county "Rutland",
    :postcode "LE15 6HP",
    :id :rutland-county}
   {:common-name "Ryedale District Council",
    :address-1 "Ryedale House",
    :address-2 "Malton",
    :town "North Yorkshire",
    :postcode "YO17 7HH",
    :county "North Yorkshire",
    :id :ryedale-district}
   {:common-name "Salford City Council",
    :address-1 "Civic Centre",
    :address-2 "Chorley Road",
    :town "Swinton",
    :county "Greater Manchester",
    :postcode "M27 5DA",
    :id :salford-city}
   {:common-name "Sandwell Metropolitan Borough Council",
    :address-1 "Sandwell Council House",
    :address-2 "Freeth Street",
    :town "Oldbury",
    :county "West Midlands",
    :postcode "B69 3DE",
    :id :sandwell-metropolitan-borough}
   {:common-name "Scarborough Borough Council",
    :address-1 "Town Hall",
    :address-2 "St Nicholas Street",
    :town "Scarborough",
    :county "North Yorkshire",
    :postcode "YO11 2HG",
    :id :scarborough-borough}
   {:common-name "Sedgemoor District Council",
    :address-1 "Bridgwater House",
    :address-2 "King Square",
    :town "Bridgwater",
    :county "Somerset",
    :postcode "TA6 3AR",
    :id :sedgemoor-district}
   {:common-name "Sefton Metropolitan Borough Council",
    :address-1 "Magdalen House",
    :address-2 "Trinity Road",
    :town "Bootle",
    :county "Merseyside",
    :postcode "L20 3NJ",
    :id :sefton-metropolitan-borough}
   {:common-name "Selby District Council",
    :address-1 "Civic Centre",
    :address-2 "Doncaster Road",
    :town "Selby",
    :county "North Yorkshire",
    :postcode "YO8 9FT",
    :id :selby-district}
   {:common-name "Sevenoaks District Council",
    :address-1 "Argyle Road",
    :address-2 "Sevenoaks",
    :town "Kent",
    :postcode "TN13 1HG",
    :county "Kent",
    :id :sevenoaks-district}
   {:common-name "Sheffield City Council",
    :address-1 "Town Hall",
    :address-2 "Pinstone Street",
    :town "Sheffield",
    :county "South Yorkshire",
    :postcode "S1 2HH",
    :id :sheffield-city}
   {:common-name "Shepway District Council",
    :address-1 "Castle Hill Avenue",
    :address-2 "Folkestone",
    :town "Kent",
    :postcode "CT20 2QY",
    :county "Kent",
    :id :shepway-district}
   {:common-name "Shropshire Council",
    :address-1 "Shirehall",
    :address-2 "Abbey Foregate",
    :town "Shrewsbury",
    :county "Shropshire",
    :postcode "SY2 6ND",
    :id :shropshire}
   {:common-name "Slough Borough Council",
    :address-1 "Observatory House",
    :address-2 "25 Windsor Road",
    :town "Slough",
    :county "Berkshire",
    :postcode "SL1 2EL",
    :id :slough-borough}
   {:common-name "Solihull Metropolitan Borough Council",
    :address-1 "Council House",
    :address-2 "Manor Square",
    :town "Solihull",
    :county "West Midlands",
    :postcode "B91 3QB",
    :id :solihull-metropolitan-borough}
   {:common-name "Somerset County Council",
    :address-1 "County Hall",
    :address-2 "The Crescent",
    :town "Taunton",
    :county "Somerset",
    :postcode "TA1 4DY",
    :id :somerset-county}
   {:common-name "South Buckinghamshire District Council",
    :address-1 "Capswood",
    :address-2 "Oxford Road",
    :town "Denham",
    :county "Buckinghamshire",
    :postcode "UB9 4LH",
    :id :south-buckinghamshire-district}
   {:common-name "South Cambridgeshire District Council",
    :address-1 "South Cambridgeshire Hall",
    :address-2 "Cambourne Business Park",
    :town "Cambourne",
    :county "Cambridgeshire",
    :postcode "CB23 6EA",
    :id :south-cambridgeshire-district}
   {:common-name "South Derbyshire District Council",
    :address-1 "Civic Offices",
    :address-2 "Civic Way",
    :town "Swadlincote",
    :county "Derbyshire",
    :postcode "DE11 0AH",
    :id :south-derbyshire-district}
   {:common-name "South Gloucestershire Council",
    :address-1 "PO Box 1954",
    :address-2 "Council Offices",
    :town "Bristol",
    :county "South Gloucestershire",
    :postcode "BS37 0DB",
    :id :south-gloucestershire}
   {:common-name "South Hams District Council",
    :address-1 "Follaton House",
    :address-2 "Plymouth Road",
    :town "Totnes",
    :county "Devon",
    :postcode "TQ9 5NE",
    :id :south-hams-district}
   {:common-name "South Holland District Council",
    :address-1 "Council Offices",
    :address-2 "Priory Road",
    :town "Spalding",
    :county "Lincolnshire",
    :postcode "PE11 2XE",
    :id :south-holland-district}
   {:common-name "South Kesteven District Council",
    :address-1 "Council Offices",
    :address-2 "St. Peter's Hill",
    :town "Grantham",
    :county "Lincolnshire",
    :postcode "NG31 6PZ",
    :id :south-kesteven-district}
   {:common-name "South Lakeland District Council",
    :address-1 "South Lakeland House",
    :address-2 "Lowther Street",
    :town "Kendal",
    :county "Cumbria",
    :postcode "LA9 4DL",
    :id :south-lakeland-district}
   {:common-name "South Norfolk District Council",
    :address-1 "South Norfolk House",
    :address-2 "Swan Lane",
    :town "Long Stratton",
    :county "Norfolk",
    :postcode "NR15 2XE",
    :id :south-norfolk-district}
   {:common-name "South Northamptonshire Council",
    :address-1 "The Forum",
    :address-2 "Moat Lane",
    :town "Towcester",
    :county "Northamptonshire",
    :postcode "NN12 6AD",
    :id :south-northamptonshire}
   {:common-name "South Oxfordshire District Council",
    :address-1 "135 Eastern Avenue",
    :address-2 "Milton Park",
    :town "Milton",
    :county "Oxfordshire",
    :postcode "OX14 4SB",
    :id :south-oxfordshire-district}
   {:common-name "South Ribble Borough Council",
    :address-1 "Civic Centre",
    :address-2 "West Paddock",
    :town "Leyland",
    :county "Lancashire",
    :postcode "PR25 1DH",
    :id :south-ribble-borough}
   {:common-name "South Somerset District Council",
    :address-1 "The Council Offices",
    :address-2 "Brympton Way",
    :town "Yeovil",
    :county "Somerset",
    :postcode "BA20 2HT",
    :id :south-somerset-district}
   {:common-name "South Staffordshire Council",
    :address-1 "Council Offices",
    :address-2 "Codsall",
    :town "Wolverhampton",
    :county "South Staffordshire",
    :postcode "WV8 1PX",
    :id :south-staffordshire}
   {:common-name "South Tyneside Council",
    :address-1 "Town Hall & Civic Offices",
    :address-2 "Westoe Road",
    :town "South Shields",
    :county "Tyne and Wear",
    :postcode "NE33 2RL",
    :id :south-tyneside}
   {:common-name "Southampton City Council",
    :address-1 "Civic Centre",
    :address-2 "Southampton",
    :town "Hampshire",
    :postcode "SO14 7LY",
    :county "Southampton",
    :id :southampton-city}
   {:common-name "Southend-on-Sea Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Victoria Avenue",
    :town "Southend-on-Sea",
    :postcode "SS2 6ER",
    :county "Essex",
    :id :southendonsea-borough}
   {:common-name "Spelthorne Borough Council",
    :address-1 "Council Offices",
    :address-2 "Knowle Green",
    :town "Staines-upon-Thames",
    :postcode "TW18 1XB",
    :county "Surrey",
    :id :spelthorne-borough}
   {:common-name "St Albans City and District Council",
    :address-1 "St Peter's Street",
    :address-2 "St Albans",
    :town "Hertfordshire",
    :postcode "AL1 3JE",
    :county "Hertfordshire",
    :id :st-albans-city-and-district}
   {:common-name "St Edmundsbury Borough Council",
    :address-1 "West Suffolk House",
    :address-2 "Western Way",
    :town "Bury St Edmunds",
    :postcode "IP33 3YU",
    :county "Suffolk",
    :id :st-edmundsbury-borough}
   {:common-name "St Helens Metropolitan Borough Council",
    :address-1 "Town Hall",
    :address-2 "Victoria Square",
    :town "St Helens",
    :postcode "WA10 1HP",
    :county "Merseyside",
    :id :st-helens-metropolitan-borough}
   {:common-name "Stafford Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Riverside",
    :town "Stafford",
    :postcode "ST16 3AQ",
    :county "Staffordshire",
    :id :stafford-borough}
   {:common-name "Staffordshire County Council",
    :address-1 "County Buildings",
    :address-2 "Martin Street",
    :town "Stafford",
    :postcode "ST16 2LH",
    :county "Staffordshire",
    :id :staffordshire-county}
   {:common-name "Staffordshire Moorlands District Council",
    :address-1 "Moorlands House",
    :address-2 "Stockwell Street",
    :town "Leek",
    :postcode "ST13 6HQ",
    :county "Staffordshire",
    :id :staffordshire-moorlands-district}
   {:common-name "Stevenage Borough Council",
    :address-1 "Daneshill House",
    :address-2 "Danestrete",
    :town "Stevenage",
    :postcode "SG1 1HN",
    :county "Hertfordshire",
    :id :stevenage-borough}
   {:common-name "Stockport Metropolitan Borough Council",
    :address-1 "Town Hall",
    :address-2 "Edward Street",
    :town "Stockport",
    :postcode "SK1 3XE",
    :county "Greater Manchester",
    :id :stockport-metropolitan-borough}
   {:common-name "Stockton-on-Tees Borough Council",
    :address-1 "Municipal Buildings",
    :address-2 "Church Road",
    :town "Stockton-on-Tees",
    :postcode "TS18 1LD",
    :county "County Durham",
    :id :stocktonontees-borough}
   {:common-name "Stoke-on-Trent City Council",
    :address-1 "Civic Centre",
    :address-2 "Glebe Street",
    :town "Stoke-on-Trent",
    :postcode "ST4 1RN",
    :county "Staffordshire",
    :id :stokeontrent-city}
   {:common-name "Strabane District Council",
    :address-1 "47 Derry Road",
    :address-2 "Strabane",
    :town "County Tyrone",
    :postcode "BT82 8DY",
    :county "County Tyrone",
    :id :strabane-district}
   {:common-name "Stratford-on-Avon District Council",
    :address-1 "Elizabeth House",
    :address-2 "Church Street",
    :town "Stratford-upon-Avon",
    :postcode "CV37 6HX",
    :county "Warwickshire",
    :id :stratfordonavon-district}
   {:common-name "Stroud District Council",
    :address-1 "Ebley Mill",
    :address-2 "Westward Road",
    :town "Stroud",
    :postcode "GL5 4UB",
    :county "Gloucestershire",
    :id :stroud-district}
   {:common-name "Suffolk County Council",
    :address-1 "Endeavour House",
    :address-2 "Russell Road",
    :town "Ipswich",
    :postcode "IP1 2BX",
    :county "Suffolk",
    :id :suffolk-county}
   {:common-name "Sunderland City Council",
    :address-1 "Civic Centre",
    :address-2 "Burdon Road",
    :town "Sunderland",
    :postcode "SR2 7DN",
    :county "Tyne and Wear",
    :id :sunderland-city}
   {:common-name "Surrey County Council",
    :address-1 "County Hall",
    :address-2 "Penrhyn Road",
    :town "Kingston upon Thames",
    :postcode "KT1 2DN",
    :county "Surrey",
    :id :surrey-county}
   {:common-name "Surrey Heath Borough Council",
    :address-1 "Surrey Heath House",
    :address-2 "Knoll Road",
    :town "Camberley",
    :postcode "GU15 3HD",
    :county "Surrey",
    :id :surrey-heath-borough}
   {:common-name "Swale Borough Council",
    :address-1 "Swale House",
    :address-2 "East Street",
    :town "Sittingbourne",
    :postcode "ME10 3HT",
    :county "Kent",
    :id :swale-borough}
   {:common-name "Swansea City and Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Oystermouth Road",
    :town "Swansea",
    :postcode "SA1 3SN",
    :county "West Glamorgan",
    :id :swansea-city-and-borough}
   {:common-name "Swindon Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Euclid Street",
    :town "Swindon",
    :postcode "SN1 2JH",
    :county "Wiltshire",
    :id :swindon-borough}
   {:common-name "Tameside Metropolitan Borough Council",
    :address-1 "Dukinfield Town Hall",
    :address-2 "King Street",
    :town "Dukinfield",
    :postcode "SK16 4LA",
    :county "Greater Manchester",
    :id :tameside-metropolitan-borough}
   {:common-name "Tamworth Borough Council",
    :address-1 "Marmion House",
    :address-2 "Lichfield Street",
    :town "Tamworth",
    :postcode "B79 7BZ",
    :county "Staffordshire",
    :id :tamworth-borough}
   {:common-name "Tandridge District Council",
    :address-1 "8 Station Road East",
    :address-2 "Oxted",
    :town "Surrey",
    :postcode "RH8 0BT",
    :county "Surrey",
    :id :tandridge-district}
   {:common-name "Taunton Deane Borough Council",
    :address-1 "The Deane House",
    :address-2 "Belvedere Road",
    :town "Taunton",
    :postcode "TA1 1HE",
    :county "Somerset",
    :id :taunton-deane-borough}
   {:common-name "Teignbridge District Council",
    :address-1 "Forde House",
    :address-2 "Brunel Road",
    :town "Newton Abbot",
    :postcode "TQ12 4XX",
    :county "Devon",
    :id :teignbridge-district}
   {:common-name "Telford & Wrekin Council",
    :address-1 "Addenbrooke House",
    :address-2 "Ironmasters Way",
    :town "Telford",
    :postcode "TF3 4NT",
    :county "Shropshire",
    :id :telford--wrekin}
   {:common-name "Tendring District Council",
    :address-1 "Town Hall",
    :address-2 "Station Road",
    :town "Clacton-on-Sea",
    :postcode "CO15 1SE",
    :county "Essex",
    :id :tendring-district}
   {:common-name "Test Valley Borough Council",
    :address-1 "Beech Hurst",
    :address-2 "Weyhill Road",
    :town "Andover",
    :postcode "SP10 3AJ",
    :county "Hampshire",
    :id :test-valley-borough}
   {:common-name "Tewkesbury Borough Council",
    :address-1 "Public Services Centre",
    :address-2 "Tewkesbury Borough Council",
    :town "Tewkesbury",
    :postcode "GL20 5TT",
    :county "Gloucestershire",
    :id :tewkesbury-borough}
   {:common-name "Thanet District Council",
    :address-1 "Cecil Street",
    :address-2 "Margate",
    :town "Kent",
    :postcode "CT9 1XZ",
    :county "Kent",
    :id :thanet-district}
   {:common-name "Three Rivers District Council",
    :address-1 "Three Rivers House",
    :address-2 "Northway",
    :town "Rickmansworth",
    :postcode "WD3 1RL",
    :county "Hertfordshire",
    :id :three-rivers-district}
   {:common-name "Thurrock Council",
    :address-1 "Civic Offices",
    :address-2 "New Road",
    :town "Grays",
    :postcode "RM17 6SL",
    :county "Essex",
    :id :thurrock}
   {:common-name "Tonbridge and Malling Borough Council",
    :address-1 "Gibson Building",
    :address-2 "Gibson Drive",
    :town "Kings Hill",
    :postcode "ME19 4LZ",
    :county "Kent",
    :id :tonbridge-and-malling-borough}
   {:common-name "Torbay Council",
    :address-1 "Town Hall",
    :address-2 "Castle Circus",
    :town "Torquay",
    :postcode "TQ1 3DR",
    :county "Devon",
    :id :torbay}
   {:common-name "Torfaen County Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Pontypool",
    :town "Torfaen",
    :postcode "NP4 6YB",
    :county "Torfaen",
    :id :torfaen-county-borough}
   {:common-name "Torridge District Council",
    :address-1 "Riverbank House",
    :address-2 "Bideford",
    :town "Devon",
    :county "Devon",
    :postcode "EX39 2QG",
    :id :torridge-district}
   {:common-name "Trafford Metropolitan Borough Council",
    :address-1 "Trafford Town Hall",
    :address-2 "Talbot Road",
    :town "Stretford",
    :county "Greater Manchester",
    :postcode "M32 0TH",
    :id :trafford-metropolitan-borough}
   {:common-name "Tunbridge Wells Borough Council",
    :address-1 "Town Hall",
    :address-2 "Mount Pleasant",
    :town "Royal Tunbridge Wells",
    :county "Kent",
    :postcode "TN1 1RS",
    :id :tunbridge-wells-borough}
   {:common-name "Uttlesford District Council",
    :address-1 "London Road",
    :address-2 "Saffron Walden",
    :town "Essex",
    :county "Essex",
    :postcode "CB11 4ER",
    :id :uttlesford-district}
   {:common-name "Vale of Glamorgan Council",
    :address-1 "Civic Offices",
    :address-2 "Holton Road",
    :town "Barry",
    :county "Vale of Glamorgan",
    :postcode "CF63 4RU",
    :id :vale-of-glamorgan}
   {:common-name "Vale of White Horse District Council",
    :address-1 "135 Eastern Avenue",
    :address-2 "Milton Park",
    :town "Milton",
    :county "Oxfordshire",
    :postcode "OX14 4SB",
    :id :vale-of-white-horse-district}
   {:common-name "Wakefield Metropolitan District Council",
    :address-1 "Town Hall",
    :address-2 "Wood Street",
    :town "Wakefield",
    :county "West Yorkshire",
    :postcode "WF1 2HQ",
    :id :wakefield-metropolitan-district}
   {:common-name "Walsall Metropolitan Borough Council",
    :address-1 "Civic Centre",
    :address-2 "Darwall Street",
    :town "Walsall",
    :county "West Midlands",
    :postcode "WS1 1DG",
    :id :walsall-metropolitan-borough}
   {:common-name "Warrington Borough Council",
    :address-1 "Town Hall",
    :address-2 "Sankey Street",
    :town "Warrington",
    :county "Cheshire",
    :postcode "WA1 1UH",
    :id :warrington-borough}
   {:common-name "Warwick District Council",
    :address-1 "Riverside House",
    :address-2 "Milverton Hill",
    :town "Leamington Spa",
    :county "Warwickshire",
    :postcode "CV32 5HZ",
    :id :warwick-district}
   {:common-name "Warwickshire County Council",
    :address-1 "Shire Hall",
    :address-2 "Warwick",
    :town "Warwickshire",
    :county "Warwickshire",
    :postcode "CV34 4RL",
    :id :warwickshire-county}
   {:common-name "Watford Borough Council",
    :address-1 "Town Hall",
    :address-2 "Watford",
    :town "Hertfordshire",
    :county "Hertfordshire",
    :postcode "WD17 3EX",
    :id :watford-borough}
   {:common-name "Waverley Borough Council",
    :address-1 "The Burys",
    :address-2 "Godalming",
    :town "Surrey",
    :county "Surrey",
    :postcode "GU7 1HR",
    :id :waverley-borough}
   {:common-name "Wealden District Council",
    :address-1 "Council Offices",
    :address-2 "Vicarage Lane",
    :town "Hailsham",
    :county "East Sussex",
    :postcode "BN27 2AX",
    :id :wealden-district}
   {:common-name "Wellingborough Borough Council",
    :address-1 "Swanspool House",
    :address-2 "Doddington Road",
    :town "Wellingborough",
    :county "Northamptonshire",
    :postcode "NN8 1BP",
    :id :wellingborough-borough}
   {:common-name "Welwyn Hatfield Council",
    :address-1 "The Campus",
    :address-2 "Welwyn Garden City",
    :town "Hertfordshire",
    :county "Hertfordshire",
    :postcode "AL8 6AE",
    :id :welwyn-hatfield}
   {:common-name "West Berkshire Council",
    :address-1 "Market Street",
    :address-2 "Newbury",
    :town "Berkshire",
    :county "West Berkshire",
    :postcode "RG14 5LD",
    :id :west-berkshire}
   {:common-name "West Devon Borough Council",
    :address-1 "Kilworthy Park",
    :address-2 "Tavistock",
    :town "Devon",
    :county "West Devon",
    :postcode "PL19 0BZ",
    :id :west-devon-borough}
   {:common-name "West Dorset District Council",
    :address-1 "South Walks House",
    :address-2 "Dorchester",
    :town "Dorset",
    :county "West Dorset",
    :postcode "DT1 1UZ",
    :id :west-dorset-district}
   {:common-name "West Lancashire Borough Council",
    :address-1 "52 Derby Street",
    :address-2 "Ormskirk",
    :town "Lancashire",
    :county "West Lancashire",
    :postcode "L39 2DF",
    :id :west-lancashire-borough}
   {:common-name "West Lindsey District Council",
    :address-1 "Guildhall",
    :address-2 "Marshall's Yard",
    :town "Gainsborough",
    :county "Lincolnshire",
    :postcode "DN21 2NA",
    :id :west-lindsey-district}
   {:common-name "West Oxfordshire District Council",
    :address-1 "Council Offices",
    :address-2 "Woodgreen",
    :town "Witney",
    :county "Oxfordshire",
    :postcode "OX28 1NB",
    :id :west-oxfordshire-district}
   {:common-name "West Somerset District Council",
    :address-1 "West Somerset House",
    :address-2 "Killick Way",
    :town "Williton",
    :county "Somerset",
    :postcode "TA4 4QA",
    :id :west-somerset-district}
   {:common-name "West Sussex County Council",
    :address-1 "County Hall",
    :address-2 "Chichester",
    :town "West Sussex",
    :county "West Sussex",
    :postcode "PO19 1RQ",
    :id :west-sussex-county}
   {:common-name "Westminster City Council",
    :address-1 "Westminster City Hall",
    :address-2 "64 Victoria Street",
    :town "London",
    :county "Greater London",
    :postcode "SW1E 6QP",
    :id :westminster-city}
   {:common-name "Weymouth and Portland Borough Council",
    :address-1 "Council Offices",
    :address-2 "North Quay",
    :town "Weymouth",
    :county "Dorset",
    :postcode "DT4 8TA",
    :id :weymouth-and-portland-borough}
   {:common-name "Wigan Metropolitan Borough Council",
    :address-1 "Town Hall",
    :address-2 "Library Street",
    :town "Wigan",
    :county "Greater Manchester",
    :postcode "WN1 1YN",
    :id :wigan-metropolitan-borough}
   {:common-name "Wiltshire Council",
    :address-1 "County Hall",
    :address-2 "Bythesea Road",
    :town "Trowbridge",
    :county "Wiltshire",
    :postcode "BA14 8JN",
    :id :wiltshire}
   {:common-name "Winchester City Council",
    :address-1 "City Offices",
    :address-2 "Colebrook Street",
    :town "Winchester",
    :county "Hampshire",
    :postcode "SO23 9LJ",
    :id :winchester-city}
   {:common-name "Wirral Council",
    :address-1 "Wallasey Town Hall",
    :address-2 "Brighton Street",
    :town "Wallasey",
    :county "Merseyside",
    :postcode "CH44 8ED",
    :id :wirral}
   {:common-name "Woking Borough Council",
    :address-1 "Civic Offices",
    :address-2 "Gloucester Square",
    :town "Woking",
    :county "Surrey",
    :postcode "GU21 6YL",
    :id :woking-borough}
   {:common-name "Wokingham Borough Council",
    :address-1 "Shute End",
    :address-2 "",
    :town "Wokingham",
    :county "Berkshire",
    :postcode "RG40 1BN",
    :id :wokingham-borough}
   {:common-name "Wolverhampton City Council",
    :address-1 "Civic Centre",
    :address-2 "St Peter's Square",
    :town "Wolverhampton",
    :county "West Midlands",
    :postcode "WV1 1SH",
    :id :wolverhampton-city}
   {:common-name "Worcester City Council",
    :address-1 "The Guildhall",
    :address-2 "High Street",
    :town "Worcester",
    :county "Worcestershire",
    :postcode "WR1 2EY",
    :id :worcester-city}
   {:common-name "Worcestershire County Council",
    :address-1 "County Hall",
    :address-2 "Spetchley Road",
    :town "Worcester",
    :county "Worcestershire",
    :postcode "WR5 2NP",
    :id :worcestershire-county}
   {:common-name "Worthing Borough Council",
    :address-1 "Town Hall",
    :address-2 "Chapel Road",
    :town "Worthing",
    :county "West Sussex",
    :postcode "BN11 1HA",
    :id :worthing-borough}
   {:common-name "Wrexham County Borough Council",
    :address-1 "Guildhall",
    :address-2 "LL11 1AY",
    :town "Wrexham",
    :county "Wrexham",
    :postcode "LL11 1AY",
    :id :wrexham-county-borough}
   {:common-name "Wychavon District Council",
    :address-1 "Civic Centre",
    :address-2 "Queen Elizabeth Drive",
    :town "Pershore",
    :county "Worcestershire",
    :postcode "WR10 1PT",
    :id :wychavon-district}
   {:common-name "Wycombe District Council",
    :address-1 "Queen Victoria Road",
    :address-2 "",
    :town "High Wycombe",
    :county "Buckinghamshire",
    :postcode "HP11 1BB",
    :id :wycombe-district}
   {:common-name "Wyre Council",
    :address-1 "Civic Centre",
    :address-2 "Breck Road",
    :town "Poulton-le-Fylde",
    :county "Lancashire",
    :postcode "FY6 7PU",
    :id :wyre}
   {:common-name "Wyre Forest District Council",
    :address-1 "Wyre Forest House",
    :address-2 "Finepoint Way",
    :town "Kidderminster",
    :county "Worcestershire",
    :postcode "DY11 7WF",
    :id :wyre-forest-district}])
