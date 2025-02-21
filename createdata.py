import mysql.connector
import random
from datetime import datetime, timedelta

# Generate mock disaster data from 2010 to 2023
random.seed(42)

disaster_types = [
    ("Flood", "Flash Flood"),
    ("Earthquake", "Tectonic"),
    ("Cyclone", "Tropical Cyclone"),
    ("Landslide", "Rockfall"),
    ("Drought", "Extreme Heat"),
    ("Tsunami", "Seismic Sea Wave"),
    ("Pandemic", "Viral Outbreak"),
]

locations = [
    "Mumbai, India", "Gujarat, India", "Odisha, India",
    "Himachal Pradesh, India", "Rajasthan, India", "Chennai, India",
    "Delhi, India", "Kolkata, India", "Bihar, India", "Uttarakhand, India"
]

mock_data = []
start_date = datetime(2010, 1, 1)

for year in range(2010, 2024):
    num_disasters = random.randint(2, 6)  # Each year has between 2 to 6 disasters
    for _ in range(num_disasters):
        disaster_type, disaster_subtype = random.choice(disaster_types)
        location = random.choice(locations)
        total_deaths = random.randint(50, 5000)
        disaster_date = start_date + timedelta(days=random.randint(0, 365))
        
        mock_data.append((disaster_date.strftime("%Y-%m-%d"), disaster_type, disaster_subtype, location, total_deaths))
    
    start_date = datetime(year + 1, 1, 1)  # Move to the next year

# Database connection
conn = mysql.connector.connect(
    host="hostname",
    user="username",
    password="pswd",
    database="databasename"
)
cursor = conn.cursor()

# Insert mock data
insert_query = "INSERT INTO disasters (Disaster_Date, Disaster_Type, Disaster_Subtype, Location, Total_Deaths) VALUES (%s, %s, %s, %s, %s)"
print(mock_data[:5])  # Print first 5 rows to verify data

cursor.executemany(insert_query, mock_data)

# Commit and close connection
conn.commit()
try:
    cursor.execute("SET NAMES utf8mb4;")
    cursor.executemany(insert_query, mock_data)
    conn.commit()
    print("Mock disaster data inserted successfully.")
except mysql.connector.Error as err:
    print(f"Error inserting data: {err}")
finally:
    cursor.close()
    conn.close()



