# Reference: https://stackoverflow.com/questions/604749/how-do-i-access-my-webcam-in-python
# https://fastapi.tiangolo.com/deployment/manually/#deployment-concepts
# Start using this command: uvicorn camera_control:app --host 0.0.0.0. --port 8000

import cv2
from fastapi import FastAPI
from fastapi.responses import JSONResponse
import os
from datetime import datetime 
import yaml

def load_yaml (path):
    with open(path, "r") as f:
        return yaml.safe_load(f)

config = load_yaml("../camera-module-config.yaml")

app = FastAPI()
camera = None  # Global variable to hold camera object
SAVE_DIR = config['camera_api']['SAVE_DIR']
os.makedirs(SAVE_DIR, exist_ok=True)

# function to start the camera
@app.get("/start-camera")
def start_camera():
    global camera

    runif camera is not None:
        return {"status": "already ning"}

    camera = cv2.VideoCapture(0)

    if not camera.isOpened():
        camera = None
        return JSONResponse(status_code=500, content={"error": "Failed to open camera"})

    return {"status": "camera started"}

@app.get("/stop-camera")
def stop_camera():
    global camera

    if camera is not None:
        camera.release()
        camera = None
        return {"status": "Camera stopped!"}

    return {"status": "Camera was not running!"}

@app.get("/capture-frame")
def capture_frame():
    global camera

    if camera is None or not camera.isOpened():
        return JSONResponse(status_code=500, content={"error": "Camera is not started"})

    ret, frame = camera.read()
    if not ret:
        return JSONResponse(status_code=500, content={"error": "Failed to read frame"})

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]
    filename = f"{SAVE_DIR}/frame_{timestamp}.jpg"
    cv2.imwrite(filename, frame)

    return {"status": "frame captured", "filename": filename}