import pytest
import sys
import os
# for some reason pytest won't find the module without this
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from xes_to_json_mapper import parse_xml
import xml.etree.ElementTree as ET
import logging


def test_parse_valid_xml():
    sample_xml = b'''<event><string key="concept:name" value="Start"/></event>'''
    result = parse_xml(sample_xml)
    assert isinstance(result, dict)
    assert result['concept:name'] == "Start"

def test_parse_invalid_xml(caplog):
    invalid_xml = b'<event><string key="concept:name" value="Start">'  # not closed
    with caplog.at_level(logging.DEBUG):
        result = parse_xml(invalid_xml)

    # Assert that the XML parsing error was logged
    assert "XML parsing error" in caplog.text
    assert "no element found" in caplog.text  # depending on the error message specifics
    assert result is None  # Ensure that the function returns None as expected
