import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    // Board dimensions
    int boardWidth = 360;
    int boardHeight = 640;

    // Images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // Bird properties
    int birdX = boardWidth / 8;
    int birdY = boardHeight / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    // Inner class representing the bird
    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    // Pipe properties
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64; // scaled by 1/6
    int pipeHeight = 512;

    // Inner class representing a pipe
    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    // Game logic variables
    Bird bird;
    int velocityX = -4; // move pipes to the left speed (simulates bird moving right)
    int velocityY = 0; // move bird up/down speed
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    // Timers
    Timer gameLoop;
    Timer placePipesTimer;
    
    // Clip for background music
    Clip backgroundMusic;
    Clip diedMusic; 

    // Game state
    boolean gameOver = false;
    double score = 0;

    // Constructor
    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // Load images
        backgroundImg = new ImageIcon(getClass().getResource("flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("bottompipe.png")).getImage();

        // Initialize bird and pipes
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        // Set up timer for placing pipes
        placePipesTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        placePipesTimer.start();

        // Set up game loop timer
        gameLoop = new Timer(1000 / 60, this); // 1000/60 = 16.6 milliseconds
        gameLoop.start();

        
        playBackgroundMusic(); // Start playing background music
        playEndMusic();
    }
    

    // Method to place pipes
    public void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    // Method to draw graphics
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    // Method to draw game elements
    public void draw(Graphics g) {
        // Background
        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);

        // Bird
        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

        // Pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // Score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }
    }

    // Method to handle game movement
    public void move() {
        // Bird movement
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);

        // Pipe movement and collision detection
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipeWidth) {
                pipe.passed = true;
                score += 0.5; // Increment score when passing a set of pipes
            }

            if (collision(bird, pipe)) {
                gameOver = true;
            }
        }

        // Check if bird is out of bounds
        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    // Play background music
    public void playBackgroundMusic() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream("Snake on the Beach - Nico Staf.wav"));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Play "died" music
    public void playEndMusic() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream("Big Explosion Cut Off.wav"));
            diedMusic = AudioSystem.getClip();
            diedMusic.open(audioInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to check collision between bird and pipe
    public boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    // ActionListener interface method
    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            // Stop timers when game is over
            placePipesTimer.stop();
            gameLoop.stop();
            // Stop background music
            backgroundMusic.stop();
            diedMusic.setFramePosition(0); // Reset the music to the beginning
            diedMusic.start(); // Play the "died" music once
            diedMusic.loop(Clip.LOOP_CONTINUOUSLY); // Loop the music
        }
    }

    // KeyListener interface methods
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // Jump when spacebar is pressed
            velocityY = -9;
            if (gameOver) {
                // Restart the game
                resetGame();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }

    // Method to reset the game state
    public void resetGame() {
        bird.y = birdY;
        velocityY = 0;
        pipes.clear();
        score = 0;
        gameOver = false;
        gameLoop.start();
        placePipesTimer.start();
        diedMusic.stop();
        backgroundMusic.setFramePosition(0);
        backgroundMusic.start();
        backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
    }
}
