from fastapi.testclient import TestClient
from camera_control import app
from datetime import datetime 

# Testing the fastapi endpoints
# Resource: https://fastapi.tiangolo.com/tutorial/testing/#testing-file

client = TestClient(app)

def test_start_camera():
    response = client.get("/start-camera")
    assert response.status_code == 200
    assert response.json() == {"status": "camera started"}

def test_starting_already_running_camera():
    client.get("/start-camera")
    response = client.get("/start-camera")
    assert response.status_code == 200
    assert response.json() == {"status": "Camera already running"}

def test_capture_frame_when_camera_off():
    client.get("/stop-camera")
    response = client.get("/capture-frame")
    assert response.status_code == 500
    assert response.json() == {"error": "Camera is not started"}

def test_capture_frame_when_camera_on():
    client.get("/start-camera")
    response = client.get("/capture-frame")
    assert response.status_code == 200

def test_stop_camera():
    response = client.get("/stop-camera")
    assert response.status_code == 200
    assert response.json() == {"status": "Camera stopped!"}

def test_stop_already_stopped_camera():
    client.get("/stop-camera")
    response = client.get("/stop-camera")
    assert response.status_code == 200
    assert response.json() ==  {"status": "Camera was not running!"}