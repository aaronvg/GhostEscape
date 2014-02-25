/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Log;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.komodo.mygdxgame.SensorFusionListener;

public class GameWorld {
	public interface WorldListener {
		public void jump();

		public void highJump();

		public void hit();

		public void coin();
	}

	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};
	private Array<Rectangle> tiles = new Array<Rectangle>();

	public static final float WORLD_WIDTH = 10;
	public static final float WORLD_HEIGHT = 15 * 20;
	public static final int WORLD_STATE_RUNNING = 0;
	public static final int WORLD_STATE_NEXT_LEVEL = 1;
	public static final int WORLD_STATE_GAME_OVER = 2;
	public static final int WORLD_STATE_DEAD = 3;

	public boolean collision = false;

	public static final Vector2 gravity = new Vector2(0, -12);

	public final Ghosty bob;
	// public final List<Platform> platforms;
	// public final List<Spring> springs;
	// public final List<Squirrel> squirrels;
	public final List<Coin> coins;
	// public Castle castle;
	public final WorldListener listener;
	public final Random rand;

	public float heightSoFar;
	public int score;
	public int state;

	public TiledMap map; // this is what the worldRenderer uses.
	public OrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Texture koalaTexture;

	private Texture ghostTexture;
	private TextureRegion ghostRegion;

	private Animation stand;
	private Animation walk;
	private Animation jump;

	SensorFusionListener sensor;
	float dampingCounter;
	
	Box2DDebugRenderer debugRenderer;
	Body circleBody;


	public GameWorld(WorldListener listener, SensorFusionListener sensor) {
		this.bob = new Ghosty(10, 84, sensor);
		// this.platforms = new ArrayList<Platform>();
		// this.springs = new ArrayList<Spring>();
		// this.squirrels = new ArrayList<Squirrel>();
		this.coins = new ArrayList<Coin>();
		this.listener = listener;
		rand = new Random();
		generateLevel();
		
	
		this.heightSoFar = 0;
		this.score = 0;
		this.state = WORLD_STATE_RUNNING;
		this.sensor = sensor;
		dampingCounter = bob.DAMPING;
		
		
		
		
		
	}

	private void generateLevel() {
		// map = new TmxMapLoader().load("data/anotherlevel.tmx");
		map = new TmxMapLoader().load("data/level.tmx");
		// float y = Platform.PLATFORM_HEIGHT / 2;
		// float maxJumpHeight = Bob.BOB_JUMP_VELOCITY * Bob.BOB_JUMP_VELOCITY /
		// (2 * -gravity.y);
		// while (y < WORLD_HEIGHT - WORLD_WIDTH / 2) {
		// int type = rand.nextFloat() > 0.8f ? Platform.PLATFORM_TYPE_MOVING :
		// Platform.PLATFORM_TYPE_STATIC;
		// float x = rand.nextFloat() * (WORLD_WIDTH - Platform.PLATFORM_WIDTH)
		// + Platform.PLATFORM_WIDTH / 2;
		//
		// Platform platform = new Platform(type, x, y);
		// platforms.add(platform);
		//
		// if (rand.nextFloat() > 0.9f && type != Platform.PLATFORM_TYPE_MOVING)
		// {
		// Spring spring = new Spring(platform.position.x, platform.position.y +
		// Platform.PLATFORM_HEIGHT / 2
		// + Spring.SPRING_HEIGHT / 2);
		// springs.add(spring);
		// }
		//
		// if (y > WORLD_HEIGHT / 3 && rand.nextFloat() > 0.8f) {
		// Squirrel squirrel = new Squirrel(platform.position.x +
		// rand.nextFloat(), platform.position.y
		// + Squirrel.SQUIRREL_HEIGHT + rand.nextFloat() * 2);
		// squirrels.add(squirrel);
		// }
		//
		// if (rand.nextFloat() > 0.6f) {
		// Coin coin = new Coin(platform.position.x + rand.nextFloat(),
		// platform.position.y + Coin.COIN_HEIGHT
		// + rand.nextFloat() * 3);
		// coins.add(coin);
		// }
		//
		// y += (maxJumpHeight - 0.5f);
		// y -= rand.nextFloat() * (maxJumpHeight / 3);
		// }
		//
		// castle = new Castle(WORLD_WIDTH / 2, y);
	}

	public void update(float deltaTime, float accelX) {
		updateBob(deltaTime, accelX);
		updatePlatforms(deltaTime);
		updateSquirrels(deltaTime);
		updateCoins(deltaTime);
		if (bob.state != Bob.BOB_STATE_HIT)
			checkCollisions();
		checkGameOver();
	}

	private void updateBob(float deltaTime, float accelX) {

		if (deltaTime == 0)
			return;
		bob.stateTime += deltaTime;

		bob.bounds.x = bob.position.x - bob.bounds.width / 2;
		bob.bounds.y = bob.position.y - bob.bounds.height / 2;
		collision = false;

		Log.d("velocity", Float.toString(bob.velocity.x));

		if (Gdx.input.isTouched()) {
			dampingCounter = bob.DAMPING;
			double deg = Math.toRadians(sensor.getAzimuth());
			// For when we rotate the world.
			// bob.velocity.x += (float) ((Math.cos(deg)) * bob.SPEED); //
			// position update
			// with deltaTime
			// bob.velocity.y += (float) ((-Math.sin(deg)) * bob.SPEED);

			// For when only the ghost rotates
			bob.velocity.x += (float) ((-Math.sin(deg)) * bob.SPEED);
			// with deltaTime
			bob.velocity.y += (float) ((Math.cos(deg)) * bob.SPEED);
			bob.directionVector.x = (float) ((-Math.sin(deg)) * 1);
			bob.directionVector.y = (float) ((Math.cos(deg)) * 1);
		} else {
			Log.d("velocityeeee", Float.toString(bob.velocity.x));
			double deg = Math.toRadians(sensor.getAzimuth());
			bob.velocity.x += (float) ((-Math.sin(deg)) * bob.SPEED);
			bob.velocity.y += (float) ((Math.cos(deg)) * bob.SPEED);
			bob.clampVelocity();
			dampingCounter *= bob.DAMPING;
			bob.velocity.scl(dampingCounter);
			
			bob.directionVector.x = (float) ((-Math.sin(deg)) * 1);
			bob.directionVector.y = (float) ((Math.cos(deg)) * 1);
		}

		bob.velocity.scl(deltaTime);
		Rectangle bobRect = rectPool.obtain();
		bobRect.set(bob.position.x, bob.position.y, bob.getBounds().width,
				bob.getBounds().height);
		
		

		int startX, startY, endX, endY;
		
		
		
		if (bob.velocity.x > 0)
			startX = endX = (int) (bob.position.x + bob.getBounds().width + bob.velocity.x);
		else
			startX = endX = (int) (bob.position.x + bob.velocity.x);

		startY = (int) (bob.position.y);
		endY = (int) (bob.position.y + bob.getBounds().height);

		
		getTiles(startX, endX, startY, endY, tiles);
		bobRect.x += bob.velocity.x;

		for (Rectangle tile : tiles) {
			if (bobRect.overlaps(tile)) {
				collision = true;
				bob.velocity.x = 0;
			//	break;
			}
		}

		bobRect.x = bob.position.x;

		if (bob.velocity.y > 0)
			startY = endY = (int) (bob.position.y + bob.getBounds().height + bob.velocity.y);
		else
			startY = endY = (int) (bob.position.y + bob.velocity.y);

		startX = (int) (bob.position.x);
		endX = (int) (bob.position.x + bob.getBounds().width);

		getTiles(startX, endX, startY, endY, tiles);
		bobRect.y += bob.velocity.y;

		for (Rectangle tile : tiles) {
			if (bobRect.overlaps(tile)) {
				collision = true;
				if (bob.velocity.y < 0) {
					bob.position.y = tile.y + tile.height;
					// bob.setGrounded(true);
				}
				bob.velocity.y = 0;
			//	break;
			}
		}
		rectPool.free(bobRect);

		bob.position.add(bob.velocity);
		bob.getBounds().x = bob.position.x;
		bob.getBounds().y = bob.position.y;

	//	bob.velocity.scl(1 / deltaTime);

		bob.update(deltaTime);
		Log.d("velocity1", Float.toString(bob.velocity.x));
		/*
		if (bob.state != Bob.BOB_STATE_HIT && bob.position.y <= 0.5f) bob.hitPlatform();
		if (bob.state != Bob.BOB_STATE_HIT) bob.velocity.x = -accelX / 10 * Bob.BOB_MOVE_VELOCITY;
		bob.update(deltaTime);
		heightSoFar = Math.max(bob.position.y, heightSoFar);
		*/
	}

	private void updatePlatforms(float deltaTime) {
		// int len = platforms.size();
		// for (int i = 0; i < len; i++) {
		// Platform platform = platforms.get(i);
		// platform.update(deltaTime);
		// if (platform.state == Platform.PLATFORM_STATE_PULVERIZING &&
		// platform.stateTime > Platform.PLATFORM_PULVERIZE_TIME) {
		// platforms.remove(platform);
		// len = platforms.size();
		// }
		// }
	}

	private void updateSquirrels(float deltaTime) {
		// int len = squirrels.size();
		// for (int i = 0; i < len; i++) {
		// Squirrel squirrel = squirrels.get(i);
		// squirrel.update(deltaTime);
		// }
	}

	private void updateCoins(float deltaTime) {
		int len = coins.size();
		for (int i = 0; i < len; i++) {
			Coin coin = coins.get(i);
			coin.update(deltaTime);
		}
	}

	private void checkCollisions() {
		checkPlatformCollisions();
		checkSquirrelCollisions();
		checkItemCollisions();
		checkCastleCollisions();
	}

	private void checkPlatformCollisions() {
		if (bob.velocity.y > 0)
			return;

		// int len = platforms.size();
		// for (int i = 0; i < len; i++) {
		// Platform platform = platforms.get(i);
		// if (bob.position.y > platform.position.y) {
		// if (bob.bounds.overlaps(platform.bounds)) {
		// bob.hitPlatform();
		// listener.jump();
		// if (rand.nextFloat() > 0.5f) {
		// platform.pulverize();
		// }
		// break;
		// }
		// }
		// }
	}

	private void checkSquirrelCollisions() {
		// int len = squirrels.size();
		// for (int i = 0; i < len; i++) {
		// Squirrel squirrel = squirrels.get(i);
		// if (squirrel.bounds.overlaps(bob.bounds)) {
		// bob.hitSquirrel();
		// listener.hit();
		// }
		// }
	}

	private void checkItemCollisions() {
		int len = coins.size();
		for (int i = 0; i < len; i++) {
			Coin coin = coins.get(i);
			if (bob.bounds.overlaps(coin.bounds)) {
				coins.remove(coin);
				len = coins.size();
				listener.coin();
				score += Coin.COIN_SCORE;
			}

		}

		if (bob.velocity.y > 0)
			return;
		//
		// len = springs.size();
		// for (int i = 0; i < len; i++) {
		// Spring spring = springs.get(i);
		// if (bob.position.y > spring.position.y) {
		// if (bob.bounds.overlaps(spring.bounds)) {
		// bob.hitSpring();
		// listener.highJump();
		// }
		// }
		// }
	}

	private void checkCastleCollisions() {
		// if (castle.bounds.overlaps(bob.bounds)) {
		// state = WORLD_STATE_NEXT_LEVEL;
		// }
	}

	private void checkGameOver() {
		if (heightSoFar - 7.5f > bob.position.y) {
			// state = WORLD_STATE_GAME_OVER;
		}
	}

	/*private void getTiles(int startX, int startY, int endX, int endY,
			Array<Rectangle> tiles) {
		Log.d("tiles", "startX " + Integer.toString(startX) + " endX " + Integer.toString(endX));
		//Log.d("tiles", "startY " + Integer.toString(startY) + " endY " + Integer.toString(endY));
		TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(0);
		rectPool.freeAll(tiles);
		tiles.clear();
		// startX *= 1/64;
		// startY *= 1/64;
		// endX *= 1/64;
		// endY *= 1/64;
		int temp;
		if(endX < startX) {
			temp = startX;
			startX = endX;
			endX = temp;
		}
		
		
		
		
		
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
	
	*/
	
	
	
	
	
	
	
	
	
	public void getTiles(int startX, int endX, int startY, int endY, Array<Rectangle> tiles) {
		rectPool.freeAll(tiles);
		tiles.clear();
		TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(0);
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
	
	
	
	

}
