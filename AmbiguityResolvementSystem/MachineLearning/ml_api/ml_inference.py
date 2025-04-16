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
    # newest_file_path = max(glob.glob(frames_folder_path), key=os.path.getmtime) # finds the newest file in the folder
    # return os.path.basename(newest_file_path)
    frame_file_paths = sorted(glob.glob(frames_folder_path), key=os.path.getmtime, reverse=True)
    number_of_frame_paths = 4
    return frame_file_paths[:number_of_frame_paths]

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

    all_probabilities = []
    image_paths = []

    for result in results: 
        if not hasattr(result, "probs") or result.probs is None:
            print("No probability returned by the model")
            continue
        image_paths.append(result.path)
        all_probabilities.append(result.probs.data.tolist())

    if not all_probabilities:
        return {"error": "No valid probabilities returned by model!"}

    avg_prob = np.mean(all_probabilities, axis=0)
    class_names = results[0].names
    class_probs = {
        class_names[i]: float(prob) for i, prob in enumerate(avg_prob)
    }

    top_class = max(class_probs, key=class_probs.get)
    top_confidence = class_probs[top_class]

    return {
        "top_class": top_class,
        "confidence": top_confidence,
        "all_class_probabilities": class_probs,
        "image_paths": image_paths
    }

def get_random_frame(n=4):
    # Assuming test frames are in 'test_frames' folder next to this script
    test_frames_path = "/Users/sejmasijaric/Documents/Bachelor Thesis/AmbiguityResolvementSystem/MachineLearning/ml_api/test-set/*.jpg"

    #frame_files = glob.glob(test_frames_path)
    frame_files = sorted(glob.glob(test_frames_path), key=os.path.getmtime, reverse=True)


    if not frame_files:
        print("No test frames found.")
        return None
    
    selected_frames = frame_files[:4]
    print("\n\Selected test frames:", )
    for frame in selected_frames:
        print(selected_frames)
    return selected_frames
