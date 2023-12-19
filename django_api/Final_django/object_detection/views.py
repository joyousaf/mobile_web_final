# Import statements remain the same
from django.http import JsonResponse
from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from PIL import Image
import numpy as np
import torch
from io import BytesIO

from torchvision import transforms

# Load the YOLOv5 model
MODEL = torch.hub.load('ultralytics/yolov5:v5.0', 'custom', path='path_to_saved_model/best.pt')

CLASS_NAMES = [
    'Apple Scab Leaf', 'Apple leaf', 'Apple rust leaf', 'Bell_pepper leaf spot', 'Bell_pepper leaf',
    'Blueberry leaf', 'Cherry leaf', 'Corn Gray leaf spot', 'Corn leaf blight', 'Corn rust leaf',
    'Peach leaf', 'Potato leaf early blight', 'Potato leaf late blight', 'Potato leaf', 'Raspberry leaf',
    'Soyabean leaf', 'Soybean leaf', 'Squash Powdery mildew leaf', 'Strawberry leaf', 'Tomato Early blight leaf',
    'Tomato Septoria leaf spot', 'Tomato leaf bacterial spot', 'Tomato leaf late blight', 'Tomato leaf mosaic virus',
    'Tomato leaf yellow virus', 'Tomato leaf', 'Tomato mold leaf', 'Tomato two spotted spider mites leaf',
    'Grape leaf black rot', 'Grape leaf'
]

@csrf_exempt
def predict(request):
    if request.method == 'POST':
        image_data = request.FILES['file'].read()
        image = Image.open(BytesIO(image_data)).convert("RGB")
        image = image.resize((256, 256))
        image_array = np.array(image)
        img_batch = np.expand_dims(image_array, 0)

        # Run predictions with the YOLOv5 model
        predictions = MODEL(img_batch)
        predicted_class = CLASS_NAMES[predictions[0]['class_ids'][0]]
        confidence = float(predictions[0]['scores'][0])

        return JsonResponse({
            'class': predicted_class,
            'confidence': confidence
        })

@csrf_exempt
def ping(request):
    return JsonResponse({'message': 'Hello, I am alive'})
