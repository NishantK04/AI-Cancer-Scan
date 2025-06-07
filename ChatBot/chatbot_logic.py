# chatbot_logic.py

"""
Advanced rule-based chatbot logic for AI scan assistance.
Add more categories or plug into ML/NLP system as needed.
"""

import re

# Intent-response dictionary
INTENTS = {
    "greeting": {
        "keywords": ["hi", "hello", "hey", "good morning", "good evening"],
        "response": "Hi! I'm your AI assistant. How can I help you with your scan report?"
    },
    "creator_info": {
        "keywords": ["who created you", "who made you", "developer", "author", "who built you"],
        "response": (
            "I was created by Nishant Kumar, a software engineer and AI enthusiast. "
            "Check his work here 👉 https://github.com/NishantK04"
        )
    },

    "bot_identity": {
        "keywords": ["who are you", "what is your name", "whats your name", "your name", "identify yourself"],
        "response": (
            "I'm Micky, your friendly AI assistant here to help you understand your scan reports."
        )
    },
    "upload_help": {
        "keywords": ["how to upload", "image upload", "can't upload", "select image", "picture", "image", "upload photo", "upload ct scan"],
        "response": (
            "To upload a scan picture, first select the scan type: lung or breast. "
            "Then choose whether to upload from your camera or gallery. "
            "Make sure the image is clear and properly focused for best results."
      )
    },


    

    "gradcam_info": {
        "keywords": ["grad-cam", "gradcam", "cam", "highlight"],
        "response": (
            "Grad-CAM shows which regions in your CT scan influenced the AI’s decision. "
            "Red areas typically indicate suspicious or abnormal regions."
        )
    },
    "confidence_info": {
        "keywords": ["confidence", "accuracy", "probability", "how sure"],
        "response": (
            "The model's prediction accuracy is about 92%, validated on real scan data. "
            "However, please consult a medical professional for final decisions."
        )
    },
    "heatmap_info": {
        "keywords": ["heatmap", "red area", "highlighted area", "colored area"],
        "response": (
            "Heatmaps show which parts of the scan the AI paid most attention to. "
            "Redder areas = more focus, possibly due to abnormalities."
        )
    },
    "next_steps": {
        "keywords": ["next step", "what should i do", "suggestion", "advice", "treatment"],
        "response": (
            "Consult an oncologist or radiologist with your CT scan and AI report. "
            "Further imaging, biopsy, or blood tests might be required."
        )
    },
    "cancer_info": {
        "keywords": ["cancer", "tumor", "abnormality", "diagnosis"],
        "response": (
            "This AI helps in detecting signs of potential cancer. "
            "Please consult a certified doctor for proper diagnosis and treatment."
        )
    },
    "scan_meaning": {
        "keywords": ["what does my scan mean", "report", "interpretation", "reading", "meaning"],
        "response": (
            "The scan highlights possible regions of concern. "
            "Use it alongside your doctor's advice for an accurate interpretation."
        )
    },
    "ct_scan_info": {
        "keywords": ["what is ct scan", "ct scan", "computed tomography", "how ct works"],
        "response": (
            "CT (Computed Tomography) scan uses X-rays to create detailed images of the inside of the body. "
            "It's commonly used for detecting abnormalities like tumors or infections."
        )
    },
    "precautions": {
        "keywords": ["before scan", "precautions", "fasting", "prepare for scan"],
        "response": (
            "Before a CT scan, avoid eating or drinking for 4–6 hours if contrast dye is used. "
            "Always inform your doctor about any allergies or medications."
        )
    },
    "report_time": {
        "keywords": ["how long report", "when will report come", "report time"],
        "response": (
            "CT scan reports usually take between 2–24 hours depending on your hospital or lab. "
            "Some centers offer faster results with AI-assisted tools."
        )
    },
    "radiologist_role": {
        "keywords": ["who reads scan", "who checks report", "radiologist"],
        "response": (
            "A radiologist is a medical doctor trained to interpret scan images and generate diagnostic reports. "
            "They verify the findings, even when AI tools are used."
        )
    },
    "symptoms_info": {
        "keywords": ["pain", "fever", "fatigue", "weight loss", "cough", "symptoms"],
        "response": (
            "Symptoms like unexplained weight loss, fatigue, cough, or pain may need further investigation. "
            "Discuss these with a doctor, especially if seen on the scan."
        )
    },
    "upload_help": {
        "keywords": ["how to upload", "image upload", "can't upload", "select image"],
        "response": (
            "To upload a scan, click the upload/select button in the app and choose a valid image or CT scan file. "
            "Make sure the file is clear and not too large."
        )
    },
    # New closing/thank you intent added here
    "closing": {
        "keywords": ["ok", "okay", "thanks", "thank you", "thankyou", "got it", "fine"],
        "response": (
            "You're welcome! If you have more questions, feel free to ask anytime."
        )
    }
}

DEFAULT_RESPONSE = (
    "I'm here to help you understand your scan report. "
    "Try asking me about heatmaps, next steps, accuracy, symptoms, or CT scan basics."
)

def match_intent(user_input: str) -> str:
    text = user_input.lower().strip()
    for intent, data in INTENTS.items():
        if any(re.search(rf"\b{re.escape(word)}\b", text) for word in data["keywords"]):
            return data["response"]
    return DEFAULT_RESPONSE

def get_response(user_input: str) -> str:
    return match_intent(user_input)
