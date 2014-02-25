package gameWorld;

import java.util.ArrayList;

import android.util.Log;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import com.komodo.mygdxgame.SensorFusionListener;



public class Ghosty extends DynamicGameObject {
	public static final int GHOST_STATE_JUMP = 0;
	public static final int GHOST_STATE_FALL = 1;
	public static final int GHOST_STATE_HIT = 2;
	public static final float GHOST_JUMP_VELOCITY = 11;
	public static final float GHOST_MOVE_VELOCITY = 20;
	public static final float GHOST_WIDTH = 0.8f;
	public static final float GHOST_HEIGHT = 0.8f;
	public static final float ACCELERATION = 12f;
	public static float DAMPING = .999f;
	
	public static final float SPEED = 8f;
	int state;
	float stateTime;
	SensorFusionListener sensor;
	float dirX;
	float dirY;
	protected Rectangle bounds;
	ArrayList<Vector2> array;
	Vector2 directionVector;

	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};
	
	public Ghosty (float x, float y, SensorFusionListener sensor) {
		super(x, y, GHOST_WIDTH, GHOST_HEIGHT);
		state = GHOST_STATE_FALL;
		stateTime = 0;
		this.sensor = sensor;
		dirX = x;
		dirY = y;
		// width is 1/32f * texture.width
		bounds = new Rectangle(position.x, position.y, GHOST_WIDTH, GHOST_HEIGHT);
		array = new ArrayList<Vector2>();
		array.add(new Vector2(x, y));
		array.add(new Vector2(x, y));
		directionVector = new Vector2();
		
		// move all this directionvector stuff somewhere else.. coalesce it.
		double deg = Math.toRadians(sensor.getAzimuth());
		directionVector.x = (float) ((-Math.sin(deg)) * 1);
		directionVector.y = (float) ((Math.cos(deg)) * 1);
		
		
	}
	
	public Rectangle getBounds() {
		return bounds;
	}
	
	public void clampVelocity() {
		velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION, ACCELERATION);
		velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION, ACCELERATION);
	}

	public void update (float deltaTime) {
		//velocity.add(World.gravity.x * deltaTime, World.gravity.y * deltaTime);
		//position.add(velocity.x * deltaTime, velocity.y * deltaTime);
		
		// Maybe just set velocities to the math.cos() etc... and that way deltaTime is taken care of.
	//	Timer t = new timer();
		
		velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION, ACCELERATION);
		velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION, ACCELERATION);
	//	if (Math.abs(velocity.x) < 1) {
	//		velocity.x = 0;
			//currentState = State.IDLE;
	//	}
		
		
		
		
		
		
		 Log.d("velocity2", Float.toString(velocity.x));
		
		
		
		
		
		
/*
		if (velocity.y > 0 && state != GHOST_STATE_HIT) {
			if (state != GHOST_STATE_JUMP) {
				state = GHOST_STATE_JUMP;
				stateTime = 0;
			}
		}

		if (velocity.y < 0 && state != GHOST_STATE_HIT) {
			if (state != GHOST_STATE_FALL) {
				state = GHOST_STATE_FALL;
				stateTime = 0;
			}
		}*/

	//	if (position.x < 0) position.x = World.WORLD_WIDTH;
	//	if (position.x > World.WORLD_WIDTH) position.x = 0;

		stateTime += deltaTime;
	}

	public void hitSquirrel () {
		velocity.set(0, 0);
		state = GHOST_STATE_HIT;
		stateTime = 0;
	}

	public void hitPlatform () {
		velocity.y = GHOST_JUMP_VELOCITY;
		state = GHOST_STATE_JUMP;
		stateTime = 0;
	}

	public void hitSpring () {
		velocity.y = GHOST_JUMP_VELOCITY * 1.5f;
		state = GHOST_STATE_JUMP;
		stateTime = 0;
	}
}
