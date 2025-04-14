import numpy as np
import cv2
from ml_model import model # import fine-tuned model
import os
import glob
import pathlib
import random

# Reference: https://stackoverflow.com/questions/50504315/finding-latest-file-in-a-folder-using-python
frames_folder_path = str(pathlib.Path(__file__).parent.resolve()) + "/captured_frames/*.jpg"

# Reference: https://stackoverflow.com/questions/50504315/finding-latest-file-in-a-folder-using-python
def get_most_recent_frame():
    newest_file_path = max(glob.glob(frames_folder_path), key=os.path.getmtime) # finds the newest file in the folder
    return os.path.basename(newest_file_path)


def model_inference ():
    # return model(get_most_recent_frame())
    # Alternatively until the connection is established with existing system
    frame_path = get_random_frame()
    if frame_path is None:
        return {"error": "No image found!"}
    
    result = model(frame_path)
    formatted_result = format_model_output(result)
    return formatted_result

# I used ChatGPT to help me format the model's output
def format_model_output(results):
    if not results:
        return {"error": "Empty result"}

    result = results[0]

    if not hasattr(result, "probs") or result.probs is None:
        return {"error": "No probabilities returned by the model"}

    probs = result.probs.data.tolist()
    names = result.names

    class_probs = {
        names[i]: float(prob) for i, prob in enumerate(probs)
    }

    top_class = max(class_probs, key=class_probs.get)
    top_confidence = class_probs[top_class]

    return {
        "top_class": top_class,
        "confidence": top_confidence,
        "all_class_probabilities": class_probs,
        "image_path": result.path
    }

def get_random_frame():
    # Assuming test frames are in 'test_frames' folder next to this script
    test_frames_path = "/Users/sejmasijaric/Documents/Bachelor Thesis/AmbiguityResolvementSystem/MachineLearning/ml_api/test-set/*.jpg"

    frame_files = glob.glob(test_frames_path)

    if not frame_files:
        print("No test frames found.")
        return None

    random_frame = random.choice(frame_files)
    print("\n\nRandomly selected frame:", random_frame)
    return random_frame
