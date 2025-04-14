from ultralytics import YOLO

# load the fine-tuned model 
model = YOLO("../../../runs/classify/train/weights/best.pt")
"""results = model("../../HandGestureTrainingDataClassified/val/injection/injection_left_bright_cluttered2_frame_0000.jpg")
print(results[0].probs)"""