# DataOnTips
DataOnTips is a JavaFX-based disaster data aggregation and visualization software that provides real-time and historical disaster insights. It integrates with a MySQL database to store and retrieve disaster records, allowing users to explore disaster trends, view affected locations, and export data for analysis.

Features
📌 Real-Time & Historical Data – View disaster records stored in a MySQL database.
🎛️ User-Friendly Interface – JavaFX-based interactive UI with smooth navigation.
📍 Detailed Insights – Click on disaster types to explore affected locations and impact details.
📊 CSV Export – Download disaster records for reporting and further analysis.
🔄 Multi-Window Navigation – Each disaster type opens a new window without closing the main application.


Technologies Used
JavaFX – For building the interactive user interface.
MySQL – To store and manage disaster data.
JDBC (Java Database Connectivity) – For database connectivity.
CSV Export – To enable users to download disaster data.


Setup Instructions
Install MySQL and create a database with the required schema.
Update Database Credentials in DataOnTips.java.
Ensure JavaFX and JDBC libraries are properly configured.
Run the Project in an IDE like Eclipse, IntelliJ, or NetBeans.


How It Works
The main dashboard displays disaster types stored in the database.
Clicking a disaster type opens a list of affected locations with event details.
Selecting a location provides specific disaster insights like subtype, casualties, and impact.
Users can export disaster records to a CSV file for external use.


Future Enhancements
🌍 Live Disaster Data Integration using external APIs.
🗺️ Map Visualization to display affected areas graphically.
🔍 Search & Filter Features for improved data exploration.


Contributing
Contributions are welcome! Feel free to submit issues or feature requests.
