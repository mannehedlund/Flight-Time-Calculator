# Flight Time Calculator
Android app for calculating flight time.

Google Play link:
https://play.google.com/store/apps/details?id=com.manne.flighttimecalculator

### Summary
The user can input all the airport names and associated flight times that are included in their trip. The geolocation corresponding to each inputted airport name is fetched from a data file (provided by OpenFlights.org) which includes ~7700 airports. This geolocation along with each associated flight time is passed as arguments in requests to the Google Time Zone API, which returns a flight-specific timezone offset. Simple arithmetic is performed on this information to provide the user with an accurate calculation of the total flight duration (and layover duration) of their trip.

#### -- Please excuse my overcommented code --
