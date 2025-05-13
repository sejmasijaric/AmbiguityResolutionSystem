import xml.etree.ElementTree as ET 
import logging
from datetime import datetime

event = ("<event>\n"
            "<string key=\"concept:name\" value=\"Donor check-in\"/>\n"
            "<date key=\"time:timestamp\" value=\"2024-09-11T15:56:16.000+00:00\"/>\n"
            "<string key=\"perform:donor\" value=\"D001\"/>\n"
            "<string key=\"location:station\" value=\"Left station\"/>\n"
        "</event>")

# to see the logs in the console
logging.basicConfig(level=logging.DEBUG)

# for one event
def parse_xml(xml_event):
    try: 
        logging.info("Converting XES to JSON")
        # parse xml string into element tree object:
        root = ET.fromstring(xml_event)
        print("Root: ", root)
        # initialise json object
        json_event = {}

        # iterate through child elements in root
        for child in root:
            logging.debug("Child tag: %s,  Child Attribute: %s", child.tag, child.attrib)
            # get key value pairs
            child_type = child.tag
            key = child.attrib.get("key")
            value = child.attrib.get("value")

            # check if neither key nor value are missing
            if key is None or value is None:
                logging.error("Missing key or value in child: %s", )
                continue
            if key=="time:timestamp":
                timestamp = datetime.fromisoformat(value.replace("Z", "+00:00"))
                value = timestamp.strftime('%Y-%m-%dT%H:%M:%S')
            json_event[key] = value
        logging.info("Final JSON event: %s", json_event)
        return json_event
    except ET.ParseError as err:
        logging.error("XML parsing error: %s", err)
    except Exception as err:
        logging.error("Exception occured when converting XES to JSON: %s", err)
