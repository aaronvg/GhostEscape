package com.komodo.mygdxgame;

import screens.MainMenuScreen;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Input.Orientation;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import com.komodo.mygdxgame.*;


public class MyGame implements Screen, ApplicationListener {

	static class Player {
		static float WIDTH;
		static float HEIGHT;
		static float MAX_VELOCITY = 10f;
		static float JUMP_VELOCITY = 40f;
		static float DAMPING = 0.87f;

		enum State {
			Standing, Walking, Jumping
		}

		final Vector2 position = new Vector2();
		final Vector2 velocity = new Vector2();
		State state = State.Walking;
		float stateTime = 0;
		boolean facesRight = true;
		boolean grounded = false;
	}

	static class Koala {
		static float WIDTH;
		static float HEIGHT;
		static float MAX_VELOCITY = 10f;
		static float JUMP_VELOCITY = 40f;
		static float DAMPING = 0.87f;

		enum State {
			Standing, Walking, Jumping
		}

		final Vector2 position = new Vector2();
		final Vector2 velocity = new Vector2();
		State state = State.Walking;
		float stateTime = 0;
		boolean facesRight = true;
		boolean grounded = false;
	}

	private TiledMap map;
	private OrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Texture koalaTexture;

	private Texture ghostTexture;
	private TextureRegion ghostRegion;

	private Animation stand;
	private Animation walk;
	private Animation jump;
	private Koala koala;

	private Player player;
	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};
	private Array<Rectangle> tiles = new Array<Rectangle>();

	private static final float GRAVITY = -2.5f;

	public SpriteBatch batch;
	private SpriteBatch batch2;
	public BitmapFont font;
	private String message = "Do something already!";
	private float highestY = 0.0f;
	static SensorFusionListener sensor;
	FPSLogger logger;

	private int playerPosX;
	private float originX;
	private float originY;
	private float textureWidth;
	private float textureHeight;

	private Rectangle glViewport;
	static final int WIDTH = 720;
	static final int HEIGHT = 720;


	public MyGame() {
		
		//setScreen(new MainMenuScreen(this));
		//sensor = MainActivity.sensorFused;

		//sensor.startSensor();

		batch = new SpriteBatch();
		font = new BitmapFont(false);
		font.setScale(5);
		font.setColor(Color.RED);
		logger = new FPSLogger();

		ghostTexture = new Texture("data/ghost_fixed.png");
		ghostRegion = new TextureRegion(ghostTexture, 0, 0, 64, 64);

		// load the koala frames, split them, and assign them to Animations
		koalaTexture = new Texture("data/koalio.png");
		TextureRegion[] regions = TextureRegion.split(koalaTexture, 18, 26)[0];
		stand = new Animation(0, regions[0]);
		jump = new Animation(0, regions[1]);
		walk = new Animation(0.15f, regions[2], regions[3], regions[4]);
		walk.setPlayMode(Animation.LOOP_PINGPONG);

		// figure out the width and height of the koala for collision
		// detection and rendering by converting a koala frames pixel
		// size into world units (1 unit == 16 pixels)
		Koala.WIDTH = 1 / 16f * regions[0].getRegionWidth();
		Koala.HEIGHT = 1 / 16f * regions[0].getRegionHeight();
		Player.WIDTH = 1 / 32f * 64;
		Player.HEIGHT = 1 / 32f * 64;
		// load the map, set the unit scale to 1/16 (1 unit == 16 pixels)
		map = new TmxMapLoader().load("data/level1.tmx");
		renderer = new OrthogonalTiledMapRenderer(map, 1 / 16f);
	
		// create an orthographic camera, shows us 30x20 units of the world
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 30, 18);
		// camera.setToOrtho(false, 60, 40);
		camera.update();

		// create the Koala we want to move around the world
		koala = new Koala();
		koala.position.set(20, 20);

		player = new Player();
		player.position.set(20, camera.position.y - 32 * 1 / 64f);

		originX = 1 / 32f * ghostTexture.getWidth() / 2f;
		originY = 1 / 32f * ghostTexture.getHeight() / 2f;
		textureWidth = 1 / 32f * 64;
		textureHeight = 1 / 32f * 64;
		
	}

	@Override
	public void dispose() {
		batch.dispose();
		font.dispose();
		sensor.stopSensor();
	}

	@Override
	public void render() {
		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		GL10 gl = Gdx.graphics.getGL10();
		batch.begin();

		int deviceAngle = Gdx.input.getRotation();
		message = "hello";

		if (Gdx.input.isPeripheralAvailable(Peripheral.Compass)) {
			message += "Azmuth:" + Float.toString(sensor.getAzimuth())// Float.toString(Gdx.input.getAzimuth())
					+ "\n";
			// message += "Pitch:" + Float.toString(Gdx.input.getPitch()) +
			// "\n";
			// message += "Roll:" + Float.toString(Gdx.input.getRoll()) + "\n";
		} else {
			message += "No compass available\n";
		}

		logger.log();

		font.drawMultiLine(batch, message, 0, h);

		batch.end();

		// get the delta time
		float deltaTime = Gdx.graphics.getDeltaTime();

		// update the koala (process input, collision detection, position
		// update)
		// updateKoala(deltaTime);
		updateGhost(deltaTime);
		// let the camera follow the koala, x-axis only
		camera.position.x = player.position.x;
		camera.position.y = player.position.y;
		float camAngle = -getCameraCurrentXYAngle(camera) + 180;
		camera.rotate(camAngle + sensor.getAzimuth() + 180);
		camera.update();


		// TODO:
		// 1. Collisions - number 1 priority
		// 2. implement awesome lighting and shaders!!! woohoo exciting as fuck!
		// 3. Start looking into how to make levels/ tiling.
		//
		// What if ghost loses speed when hitting a wall instead of dying..

		// baby ghosts / trail.
		// maybe ghost activates light and a bunch of light come out of its
		// mouth and eyes and it
		// looks angry! and it kills everything in sight with this light (or
		// just helps you see). (maybe activate this light with sound
		// "paaaaaaaaaaam" hahahaha)
		// one level can be really really dark. with monsters that light up and
		// light off at intervals,
		// so you only see them once and they disappear but they're still tehre
		// so you guess where they were!

		// set the tile map rendere view based on what the
		// camera sees and render the map
		// create an orthographic camera, shows us 30x20 units of the world
		

		//renderer.setView(camera);
		// Renders a bit more than what the camera sees.
		renderer.setView(camera.combined, camera.position.x - 20, camera.position.y - 20, camera.viewportWidth + 40, camera.viewportHeight + 60);

		renderer.render();

		batch2 = (SpriteBatch) renderer.getSpriteBatch();
		batch2.begin();

		batch2.draw(ghostRegion, player.position.x - 32 * (1 / 32f),
				player.position.y, originX, originY, textureWidth,
				textureHeight, 1, 1, -sensor.getAzimuth() - 90, false);

		batch2.end();

		// render the koala
		// renderKoala(deltaTime);

		// renderGhost(deltaTime);

	}

	public float getCameraCurrentXYAngle(OrthographicCamera cam) {
		return (float) Math.atan2(cam.up.x, cam.up.y)
				* MathUtils.radiansToDegrees;
	}

	private Vector2 tmp = new Vector2();

	private void updateGhost(float deltaTime) {

		if (deltaTime == 0)
			return;
		player.stateTime += deltaTime;

		double deg = Math.toRadians(sensor.getAzimuth());

		// When we go counterclockwise the angle is negative.
		player.position.x += (Math.cos(deg)) * .16; // THE SPEED
		player.position.y += (-Math.sin(deg)) * .16;

		// check input and apply to velocity & state
		// if ((Gdx.input.isKeyPressed(Keys.SPACE) || isTouched(0.75f, 1)) &&
		// player.grounded)
		// {
		// player.velocity.y += Koala.JUMP_VELOCITY;
		// player.state = Koala.State.Jumping;
		// player.grounded = false;
		// }

		// if (Gdx.input.isKeyPressed(Keys.RIGHT) ||
		// Gdx.input.isKeyPressed(Keys.D) || isTouched(0.25f, 0.5f))
		// {
		player.velocity.x = Player.MAX_VELOCITY;
		// if (koala.grounded)
		// koala.state = Koala.State.Walking;
		player.facesRight = true;
		// }

		// apply gravity if we are falling
		// player.velocity.add(0, GRAVITY);

		// clamp the velocity to the maximum, x-axis only
		if (Math.abs(player.velocity.x) > Player.MAX_VELOCITY) {
			player.velocity.x = Math.signum(player.velocity.x)
					* Player.MAX_VELOCITY;
		}

		// clamp the velocity to 0 if it's < 1, and set the state to standign
		if (Math.abs(koala.velocity.x) < 1) {
			player.velocity.x = 0;
			// if (player.grounded)
			// player.state = Koala.State.Standing;
		}

		// multiply by delta time so we know how far we go
		// in this frame
		player.velocity.mul(deltaTime);

		// perform collision detection & response, on each axis, separately
		// if the koala is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle koalaRect = rectPool.obtain();
		koalaRect.set(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
		int startX, startY, endX, endY;
		if (player.velocity.x > 0)
		{
			startX = endX = (int) (player.position.x + Player.WIDTH + player.velocity.x);
		}
		else
		{
			startX = endX = (int) (player.position.x + player.velocity.x);
		}
		startY = (int) (player.position.y);
		endY = (int) (player.position.y + Player.HEIGHT);
		getTiles(startX, startY, endX, endY, tiles);
		koalaRect.x += player.velocity.x;
		for (Rectangle tile : tiles)
		{
			if (koalaRect.overlaps(tile))
			{
				player.velocity.x = 0;
				break;
			}
		}
		koalaRect.x = player.position.x;

		// if the koala is moving upwards, check the tiles to the top of it's
		// top bounding box edge, otherwise check the ones to the bottom
		if (player.velocity.y > 0)
		{
			startY = endY = (int) (player.position.y + Player.HEIGHT + player.velocity.y);
		}
		else
		{
			startY = endY = (int) (player.position.y + player.velocity.y);
		}
		startX = (int) (player.position.x);
		endX = (int) (player.position.x + Player.WIDTH);
		getTiles(startX, startY, endX, endY, tiles);
		koalaRect.y += player.velocity.y;
		for (Rectangle tile : tiles)
		{
			if (koalaRect.overlaps(tile))
			{
				// we actually reset the koala y-position here
				// so it is just below/above the tile we collided with
				// this removes bouncing :)
				if (player.velocity.y > 0)
				{
					player.position.y = tile.y - Player.HEIGHT;
					// we hit a block jumping upwards, let's destroy it!
					TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(1);
					layer.setCell((int) tile.x, (int) tile.y, null);
				}
				else
				{
					player.position.y = tile.y + tile.height;
					// if we hit the ground, mark us as grounded so we can jump
					player.grounded = true;
				}
				player.velocity.y = 0;
				break;
			}
		}
		rectPool.free(koalaRect);
		

		// unscale the velocity by the inverse delta time and set
		// the latest position
		player.position.add(player.velocity);
		player.velocity.mul(1 / deltaTime);

		// Apply damping to the velocity on the x-axis so we don't
		// walk infinitely once a key was pressed
		player.velocity.x *= Player.DAMPING;
	}

	private void updateKoala(float deltaTime) {
		if (deltaTime == 0)
			return;
		koala.stateTime += deltaTime;

		// check input and apply to velocity & state
		if ((Gdx.input.isKeyPressed(Keys.SPACE) || isTouched(0.75f, 1))
				&& koala.grounded) {
			koala.velocity.y += Koala.JUMP_VELOCITY;
			koala.state = Koala.State.Jumping;
			koala.grounded = false;
		}

		if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)
				|| isTouched(0, 0.25f)) {
			koala.velocity.x = -Koala.MAX_VELOCITY;
			if (koala.grounded)
				koala.state = Koala.State.Walking;
			koala.facesRight = false;
		}

		if (Gdx.input.isKeyPressed(Keys.RIGHT)
				|| Gdx.input.isKeyPressed(Keys.D) || isTouched(0.25f, 0.5f)) {
			koala.velocity.x = Koala.MAX_VELOCITY;
			if (koala.grounded)
				koala.state = Koala.State.Walking;
			koala.facesRight = true;
		}

		// apply gravity if we are falling
		// koala.velocity.add(0, GRAVITY);

		// clamp the velocity to the maximum, x-axis only
		if (Math.abs(koala.velocity.x) > Koala.MAX_VELOCITY) {
			koala.velocity.x = Math.signum(koala.velocity.x)
					* Koala.MAX_VELOCITY;
		}

		// clamp the velocity to 0 if it's < 1, and set the state to standign
		if (Math.abs(koala.velocity.x) < 1) {
			koala.velocity.x = 0;
			if (koala.grounded)
				koala.state = Koala.State.Standing;
		}

		// multiply by delta time so we know how far we go
		// in this frame
		koala.velocity.mul(deltaTime);

		// perform collision detection & response, on each axis, separately
		// if the koala is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle koalaRect = rectPool.obtain();
		koalaRect.set(koala.position.x, koala.position.y, Koala.WIDTH,
				Koala.HEIGHT);
		int startX, startY, endX, endY;
		if (koala.velocity.x > 0) {
			startX = endX = (int) (koala.position.x + Koala.WIDTH + koala.velocity.x);
		} else {
			startX = endX = (int) (koala.position.x + koala.velocity.x);
		}
		startY = (int) (koala.position.y);
		endY = (int) (koala.position.y + Koala.HEIGHT);
		getTiles(startX, startY, endX, endY, tiles);
		koalaRect.x += koala.velocity.x;
		for (Rectangle tile : tiles) {
			if (koalaRect.overlaps(tile)) {
				koala.velocity.x = 0;
				break;
			}
		}
		koalaRect.x = koala.position.x;

		// if the koala is moving upwards, check the tiles to the top of it's
		// top bounding box edge, otherwise check the ones to the bottom
		if (koala.velocity.y > 0) {
			startY = endY = (int) (koala.position.y + Koala.HEIGHT + koala.velocity.y);
		} else {
			startY = endY = (int) (koala.position.y + koala.velocity.y);
		}
		startX = (int) (koala.position.x);
		endX = (int) (koala.position.x + Koala.WIDTH);
		getTiles(startX, startY, endX, endY, tiles);
		koalaRect.y += koala.velocity.y;
		for (Rectangle tile : tiles) {
			if (koalaRect.overlaps(tile)) {
				// we actually reset the koala y-position here
				// so it is just below/above the tile we collided with
				// this removes bouncing :)
				if (koala.velocity.y > 0) {
					koala.position.y = tile.y - Koala.HEIGHT;
					// we hit a block jumping upwards, let's destroy it!
					TiledMapTileLayer layer = (TiledMapTileLayer) map
							.getLayers().get(1);
					layer.setCell((int) tile.x, (int) tile.y, null);
				} else {
					koala.position.y = tile.y + tile.height;
					// if we hit the ground, mark us as grounded so we can jump
					koala.grounded = true;
				}
				koala.velocity.y = 0;
				break;
			}
		}
		rectPool.free(koalaRect);

		// unscale the velocity by the inverse delta time and set
		// the latest position
		koala.position.add(koala.velocity);
		koala.velocity.mul(1 / deltaTime);

		// Apply damping to the velocity on the x-axis so we don't
		// walk infinitely once a key was pressed
		koala.velocity.x *= Koala.DAMPING;

	}

	private void getTiles(int startX, int startY, int endX, int endY,
			Array<Rectangle> tiles) {
		TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(1);
		rectPool.freeAll(tiles);
		tiles.clear();
		for (int y = startY; y <= endY; y++) {
			for (int x = startX; x <= endX; x++) {
				Cell cell = layer.getCell(x, y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
			}
		}
	}

	private void renderKoala(float deltaTime) {
		// based on the koala state, get the animation frame
		TextureRegion frame = null;
		switch (koala.state) {
		case Standing:
			frame = stand.getKeyFrame(koala.stateTime);
			break;
		case Walking:
			frame = walk.getKeyFrame(koala.stateTime);
			break;
		case Jumping:
			frame = jump.getKeyFrame(koala.stateTime);
			break;
		}

		// draw the koala, depending on the current velocity
		// on the x-axis, draw the koala facing either right
		// or left
		SpriteBatch batch = (SpriteBatch) renderer.getSpriteBatch();
		batch.begin();
		if (koala.facesRight) {
			batch.draw(frame, koala.position.x, koala.position.y, Koala.WIDTH,
					Koala.HEIGHT);
		} else {
			batch.draw(frame, koala.position.x + Koala.WIDTH, koala.position.y,
					-Koala.WIDTH, Koala.HEIGHT);
		}

		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		batch.dispose();
		batch = new SpriteBatch();
		String resolution = Integer.toString(width) + ","
				+ Integer.toString(height);
		Gdx.app.log("MJF", "Resolution changed " + resolution);

	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	private boolean isTouched(float startX, float endX) {
		// check if any finge is touch the area between startX and endX
		// startX/endX are given between 0 (left edge of the screen) and 1
		// (right edge of the screen)
		for (int i = 0; i < 2; i++) {
			float x = Gdx.input.getX() / (float) Gdx.graphics.getWidth();
			if (Gdx.input.isTouched(i) && (x >= startX && x <= endX)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void render(float delta) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void create() {
		// TODO Auto-generated method stub
		
	}
	
	public static SensorFusionListener getSensor() {
		return sensor;
	}

}
