package gameWorld;

import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
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
	public static final float ACCELERATION = 8f;
	public static float DAMPING = .999f;

	public static final float SPEED = 8f;
	int state;
	float stateTime;
	SensorFusionListener sensor;
	float dirX;
	float dirY;
	protected Rectangle bounds;
	Vector2 directionVector;
	boolean first;
	float angleBaby;
	float dampingCounter;
	Vector2 position2;

	Body circleBody;
	World world;

	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};

	public Ghosty(float x, float y, SensorFusionListener sensor, World world2) {
		super(x, y, GHOST_WIDTH, GHOST_HEIGHT);
		state = GHOST_STATE_FALL;
		stateTime = 0;
		this.sensor = sensor;
		dirX = x;
		dirY = y;
		// width is 1/32f * texture.width
		bounds = new Rectangle(position.x, position.y, GHOST_WIDTH,
				GHOST_HEIGHT);
		position2 = new Vector2(x, y);

		directionVector = new Vector2();

		dampingCounter = 0;
		// Box2dstuff: create a circular rigidBody for our ghost
		// Create definition of a collision body (a square)
		world = world2;
		BodyDef circleDef = new BodyDef();
		circleDef.type = BodyType.DynamicBody;
		circleDef.position.set(x + .5f, y + .5f);

		// Create shape for that definition
		circleBody = world.createBody(circleDef);
		CircleShape circleShape = new CircleShape();
		circleShape.setRadius(.55f);
		circleBody.setFixedRotation(true);

		// Unite them in one fixture object.
		FixtureDef circleFixture = new FixtureDef();
		circleFixture.shape = circleShape;
		circleFixture.density = .4f;
		circleFixture.friction = .2f;
		circleFixture.restitution = .6f;
		circleBody.createFixture(circleFixture);

		// move all this directionvector stuff somewhere else.. coalesce it.
		angleBaby = sensor.getAzimuth();
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

	public void update(float deltaTime) {
		//clampVelocity();
		float lerp = 0.08f;
		position2.x += (position.x - position2.x) * lerp;
		position2.y += (position.y - position2.y) * lerp;

		angleBaby = LerpDegrees(angleBaby, sensor.getAzimuth(), .08f);

		// ------------------------box2dstuff
		// circleBody.
		// physics update
		
		float ACCELERATION = 5;
		float damping = .999f;
		double deg = Math.toRadians(sensor.getAzimuth());
		if (Gdx.input.isTouched()) {
			dampingCounter += .06f;
			if (dampingCounter > damping) {
				dampingCounter = damping;
			}
			// dampingCounter = damping;
			velocity.x = (float) ((-Math.sin(deg)) * 8 * dampingCounter);
			velocity.y = (float) ((Math.cos(deg)) * 8 * dampingCounter);
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);
		} else {
			velocity.x = (float) ((-Math.sin(deg)) * 8);
			velocity.y = (float) ((Math.cos(deg)) * 8);
			dampingCounter *= damping;
			velocity.scl(dampingCounter);
			//Log.d("damping", "hi" + Float.toString(dampingCounter));
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);

		}
		
		directionVector.x = (float) ((-Math.sin(deg)) * 8);
		directionVector.y = (float) ((Math.cos(deg)) * 8);

		position.x = circleBody.getPosition().x;
		position.y = circleBody.getPosition().y;
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

		// if (position.x < 0) position.x = World.WORLD_WIDTH;
		// if (position.x > World.WORLD_WIDTH) position.x = 0;

		stateTime += deltaTime;
	}
	
	// for when the world itself stays the same (we are rotating it but it looks the same)
	public void update2(float deltaTime) {
		//clampVelocity();
		float lerp = 0.08f;
		position2.x += (position.x - position2.x) * lerp;
		position2.y += (position.y - position2.y) * lerp;

		angleBaby = LerpDegrees(angleBaby, sensor.getAzimuth(), .08f);

		// ------------------------box2dstuff
		// circleBody.
		// physics update
		
		float ACCELERATION = 5;
		float damping = .999f;
		double deg = Math.toRadians(sensor.getAzimuth() + 90);
		if (Gdx.input.isTouched()) {
			dampingCounter += .06f;
			if (dampingCounter > damping) {
				dampingCounter = damping;
			}
			// dampingCounter = damping;
			velocity.x = (float) ((Math.cos(deg)) * 8 * dampingCounter); // the angles are the only change for rotationmodes
			velocity.y = (float) ((-Math.sin(deg)) * 8 * dampingCounter);
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);
			
		} else {
			velocity.x = (float) ((Math.cos(deg)) * 8);
			velocity.y = (float) ((-Math.sin(deg)) * 8);
			dampingCounter *= damping;
			velocity.scl(dampingCounter);
			//Log.d("damping", "hi" + Float.toString(dampingCounter));
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);

		}
		
		directionVector.x = (float) ((Math.cos(deg)) * 8);
		directionVector.y = (float) ((-Math.sin(deg)) * 8);

		position.x = circleBody.getPosition().x;
		position.y = circleBody.getPosition().y;
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

		// if (position.x < 0) position.x = World.WORLD_WIDTH;
		// if (position.x > World.WORLD_WIDTH) position.x = 0;

		stateTime += deltaTime;
	}
	
	
	
	
	
	
	
	

	public static float LerpDegrees(float start, float end, float amount) {
		float difference = Math.abs(end - start);
		if (difference > 180) {
			// We need to add on to one of the values.
			if (end > start) {
				// We'll add it on to start...
				start += 360;
			} else {
				// Add it on to end.
				end += 360;
			}
		}

		// Interpolate it.
		float value = (start + ((end - start) * amount));

		// Wrap it..
		float rangeZero = 360;

		if (value >= 0 && value <= 360)
			return value;

		return (value % rangeZero);
	}

	public void hitSquirrel() {
		velocity.set(0, 0);
		state = GHOST_STATE_HIT;
		stateTime = 0;
	}

	public void hitPlatform() {
		velocity.y = GHOST_JUMP_VELOCITY;
		state = GHOST_STATE_JUMP;
		stateTime = 0;
	}

	public void hitSpring() {
		velocity.y = GHOST_JUMP_VELOCITY * 1.5f;
		state = GHOST_STATE_JUMP;
		stateTime = 0;
	}
}
