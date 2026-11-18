from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/predict', methods=['POST'])
def predict():
    """
    Accepts: { "features": [distance_meters, road_type, time_of_day_minutes, traffic_level, urgency_level] }
    Returns: { "predicted_eta_seconds": <float> }
    """
    data = request.get_json(force=True, silent=True)
    if not data or 'features' not in data:
        return jsonify({'error': 'Missing features list in payload'}), 400

    features = data['features']
    if not isinstance(features, list) or len(features) < 5:
        return jsonify({'error': 'Features must be a list of 5 numeric values'}), 400

    try:
        distance_meters = float(features[0])
        traffic_delay = float(features[3])
    except (ValueError, TypeError):
        distance_meters = 1000.0
        traffic_delay = 0.0

    # Base duration assuming ~40 km/h (11.1 m/s) average speed + traffic delay
    base_duration = distance_meters / 11.1
    predicted_eta = round(base_duration + traffic_delay, 1)

    return jsonify({'predicted_eta_seconds': predicted_eta})

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'UP'})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)
