# CinemaPostersAnywhere

**CinemaPostersAnywhere** is an Android application that displays movie posters and information dynamically, providing users with an engaging way to explore films and related content. The app integrates with various APIs, including YouTube, Steam, and TMDB, to enhance user experience and functionality.

## Key Features:

- **Dynamic Movie Posters**: Showcases current movie posters with real-time updates, allowing users to see the latest films.

- **Dynamic Game Posters**: Displays detailed information about Steam games during gameplay, including:
  - **Game Poster**: Visual representation of the game.
  - **Game Description**: Engaging summary highlighting gameplay mechanics, storyline, and key features.
  - **Additional Game Information**: Includes essential details such as release date, system requirements, and genres, giving users a comprehensive understanding of the game.
  - **Trailers Related to the Game**: Fetches and displays random video trailers associated with the currently played game, enhancing the gaming experience with visual insights.

- **API Integrations**:
  - **YouTube API**: Fetches related video content for movies, allowing users to watch trailers or clips directly within the app.
  - **Steam API**: Provides up-to-date information about Steam games when played, ensuring users have access to the latest game details.
  - **TMDB API**: Offers comprehensive movie and TV show information, including ratings, genres, and overviews.

- **Flask API Integration**: Communicates with a Flask backend to retrieve and display movie data, ensuring a seamless experience for users.

- **Local Storage**: Saves movie poster images locally to improve load times and allow offline access.

- **Slideshow Functionality**: Automatically cycles through movie posters, enhancing user engagement and interaction.

- **Progress Indicator**: Displays loading progress while fetching movie data and video content, ensuring users are informed during data retrieval.

## Technologies Used:

- **Programming Languages**: Java, XML (for Android UI)
- **Libraries**: 
  - Glide for image loading
  - Retrofit for API calls
  - Room for local database management
- **Backend**: Flask (Python)

## Getting Started:

To run this project locally, clone the repository and follow the setup instructions provided in the documentation.
