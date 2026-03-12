Assistant juridique vulgarisant les contrats d'assurance


cd backend-ai
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

cp .env.example .env

python -m uvicorn app.main:app --reload --port 8000
