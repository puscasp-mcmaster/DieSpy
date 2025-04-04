def main():
    from ultralytics import YOLO
    import torch

    model_path = "runs/detect/4_2_200_s/weights/best.pt"
    
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = YOLO(model_path)
    model.export(format="tflite", device=device)

if __name__ == '__main__':
    main()