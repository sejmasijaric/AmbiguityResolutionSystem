from ultralytics import YOLO
import logging

# load the fine-tuned model 
logging.info("Loading classification model...")
model = YOLO("../../../runs/classify/train/weights/best.pt")