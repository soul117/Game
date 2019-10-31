package logic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.game.R;

import java.util.ArrayList;
import java.util.Random;

import data.PlayerInfo;
import models.AGameObject;
import models.Ball;
import models.Bascket;
import models.Button;
import models.Label;
import settings.ApplicationSettings;
import settings.UserSettings;

//Class GameView extends SurfaceView methods and implements Runnable, IGameLogic interface
public class GameView extends SurfaceView implements Runnable, IGameLogic {

    //volatile means that "playing" vulnerable threads becomes visible to all other threads
    private static volatile boolean playing;
    //initializing gameThread
    private Thread gameThread = null;

    //adding all objects to this list
    private ArrayList<AGameObject> gameObjects;

    //These objects will be used for drawing
    //Paint define exactly how graphic primitives should be displayed on a bitmap (color, stroke, style etc.)
    private Paint paint;
    //Canvas works with pixels graphics
    private Canvas canvas;
    //SurfaceHolder uses to updating background image
    private SurfaceHolder surfaceHolder;
    //start time for game =0
    private long startTime = 0;

    //creating object which allows us to access to application-specific functions
    public GameView(Context context) {
        //reference to parent class
        super(context);

        //initializing drawing objects
        surfaceHolder = getHolder();
        paint = new Paint();
        createObject();
        //to control the number of frames for smooth play (60 fps)
        startTime = System.nanoTime();
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static void setPlaying(boolean playing) {
        GameView.playing = playing;
    }


    //pause the game
    public void pause() {
        setPlaying(false);
        try {
            // join() method allows one thread to wait until another thread completes its execution
            gameThread.join();
            //interrupt thread (so pause)
        } catch (InterruptedException e) {
        }
    }

    //resume game
    public void resume() {
        setPlaying(true);
        gameThread = new Thread(this);
        //game thread starts (so game continues)
        gameThread.start();
    }


    @Override
    //update and draw object while we are playing. Also control frames
    public void run() {
        while (isPlaying()) {
            userActionDispatcher();
            update();
            draw();
            control();
        }
    }

    @Override
    public void userActionDispatcher() {
        //if we really click on screen
        if (PlayerInfo.isPlayerClick()) {
            isClickOn("ALL");

            //reset screen is clicked
            PlayerInfo.playerClickOnScreen(false);
        }
    }

    private void update() {

        //updating objects position
        for (int i = 0; i < gameObjects.size(); i++) {
            gameObjects.get(i).update();
        }
    }

    private void draw() {
        //checking if surface is valid
        if (surfaceHolder.getSurface().isValid()) {
            //locking the canvas
            try {
                canvas = surfaceHolder.lockCanvas();

                //drawing a background color for canvas
                canvas.drawColor(Color.rgb(188, 234, 250));
                //drawing our game objects on canvas
                for (int i = 0; i < gameObjects.size(); i++) {
                    gameObjects.get(i).draw(canvas);
                }

            } finally {
                //Unlocking the canvas
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void control() {
        long now = System.nanoTime();
        // Interval to redraw game
        // (Change nanoseconds to milliseconds)
        long waitTime = (now - startTime) / 1000000;
        //17 millisec is 1 frame
        if (waitTime < 17 && waitTime >= 0) {
            waitTime = 17 - waitTime; // Millisecond.

            try {
                // Sleep.
                gameThread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        startTime = System.nanoTime();
    }


    private void createObject() {

        //create new array of elements
        gameObjects = new ArrayList<>();
        //добавляем в игру кнопку выхода (перехода на главную активность)
        gameObjects.add(new Button(this, new PointF(0, 20), 100, 100, R.drawable.back_to_black));

        //creating label for our boxes
        gameObjects.add(new Label(this));
        //setting positions for our boxes
        // Height and Width on the screen
        gameObjects.add(new Bascket(this, new PointF(0, ApplicationSettings.getScreenHeight() - 350), 300, 300, R.drawable.box_green));
        gameObjects.add(new Bascket(this, new PointF(ApplicationSettings.getScreenWidth() - 350, ApplicationSettings.getScreenHeight() - 350), 300, 300, R.drawable.box_red));
        //creating new ball object
        createNewBallRandomly(this.getContext(), null);
    }

    @Override
    //this class is creating new random falling ball
    public void createNewBallRandomly(Context context, Ball ball) {
        //create an object of a class
        Random rnd = new Random();
        //if variable ball not null
        if (ball != null) {
            //to prevent new ball
            gameObjects.remove(gameObjects.indexOf(ball));
        }

        int r = rnd.nextInt(20);
        //generating new random color ball
        //dependent of color
        if (r >= 8) {
            gameObjects.add(0, new Ball(this, UserSettings.getSpeed(), 75, Color.WHITE, R.drawable.ball_green));
        } else {
            gameObjects.add(0, new Ball(this, UserSettings.getSpeed(), 75, Color.WHITE, R.drawable.ball_red));
        }
    }


    @Override
    public void isClickOn(String typeOfObject) {
        //looking through all game objects on screen
        //and cheeking their size
        for (int i = 0; i < gameObjects.size(); i++) {
            AGameObject item = gameObjects.get(i);
            if (typeOfObject != null && (typeOfObject.equals("ALL") || item.getType().equals(typeOfObject))) {
                Rect bounds = item.getBounds();

                if (
                        PlayerInfo.getUserClickCoordinates().y >= bounds.top &&
                                PlayerInfo.getUserClickCoordinates().x >= bounds.left &&
                                PlayerInfo.getUserClickCoordinates().y <= bounds.bottom &&
                                PlayerInfo.getUserClickCoordinates().x <= bounds.right
                ) {
                    item.onClick(this, null);
                }
            }
        }
    }

    @Override
    public Ball getFirstBall() {
        for (int i = 0; i < gameObjects.size(); i++) {
            if (gameObjects.get(i).getType().equals("BALL")) {
                return (Ball) gameObjects.get(i);
            }
        }

        return null;
    }

    @Override
    //dispatchTouchEvent is working with screen touches
    public boolean dispatchTouchEvent(MotionEvent event) {
        //getting X, Y coordinates
        PlayerInfo.setUserClickCoordinates(new Point((int) event.getX(), (int) event.getY()));
        //get actual user action on screen
        PlayerInfo.setMotionEvent(event.getAction());
        //Increase clicks counter by 1
        PlayerInfo.setClickCount(PlayerInfo.getClickCount() + 1);
        //was really clicked? - true
        PlayerInfo.playerClickOnScreen(true);
        //if screen was touched
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //print "TOUCH DOWN!" in console
            System.out.println("TOUCH DOWN!");
        }
        //return current instance of the parent class dispatchTouchEvent
        return super.dispatchTouchEvent(event);
    }
}