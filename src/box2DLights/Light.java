package box2DLights;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;

/**
 * @author kalle
 * 
 */
public abstract class Light {

        static final Color DefaultColor = new Color(0.75f, 0.75f, 0.5f, 0.75f);
        private boolean active = true;
        protected boolean soft = true;
        protected boolean xray = false;
        protected boolean staticLight = false;
        protected float softShadowLenght = 2.5f;

        protected RayHandler rayHandler;
        protected boolean culled = false;
        protected int rayNum;
        protected int vertexNum;
        protected float distance;
        protected float direction;

        protected Color color = new Color();
        protected Mesh lightMesh;
        protected Mesh softShadowMesh;

        protected float colorF;

        final static int MIN_RAYS = 3;

        float segments[];
        float[] mx;
        float[] my;
        float[] f;
        int m_index = 0;

        public Light(RayHandler rayHandler, int rays, Color color, float directionDegree,
                        float distance) {

                rayHandler.lightList.add(this);
                this.rayHandler = rayHandler;
                setRayNum(rays);
                this.direction = directionDegree;
                distance *= RayHandler.gammaCorrectionParameter;
                this.distance = distance < 0.01f ? 0.01f : distance;
                setColor(color);
        }

        /**
         * setColor(Color newColor) { rgb set the color and alpha set intesity NOTE:
         * you can also use colorless light with shadows(EG 0,0,0,1)
         * 
         * @param newColor
         */
        public void setColor(Color newColor) {
                if (newColor != null) {
                        color.set(newColor);
                        colorF = color.toFloatBits();
                } else {
                        color = DefaultColor;
                        colorF = DefaultColor.toFloatBits();
                }
                if (staticLight)
                        staticUpdate();
        }

        /**
         * set Color(float r, float g, float b, float a) rgb set the color and alpha
         * set intesity NOTE: you can also use colorless light with shadows(EG
         * 0,0,0,1)
         * 
         * @param r
         *            red
         * @param g
         *            green
         * @param b
         *            blue
         * @param a
         *            intesity
         */
        public void setColor(float r, float g, float b, float a) {
                color.r = r;
    color.g = g;
    color.b = b;
    color.a = a;
                colorF = color.toFloatBits();
                if (staticLight)
                        staticUpdate();
        }

        /**
         * setDistance(float dist) MIN capped to 1cm
         * 
         * @param dist
         */
        public void setDistance(float dist) {
        }

        abstract void update();

        abstract void render();

        public abstract void setDirection(float directionDegree);

        public void remove() {
                rayHandler.lightList.removeValue(this, false);
                lightMesh.dispose();
                softShadowMesh.dispose();
        }

        /**
         * attach positional light to automatically follow body. Position is fixed
         * to given offset
         * 
         * NOTE: does absolute nothing if directional light
         */
        public abstract void attachToBody(Body body, float offsetX, float offSetY);

        /**
         * @return attached body or null if not set.
         * 
         *         NOTE: directional light allways return null
         */
        public abstract Body getBody();

        /**
         * set light starting position
         * 
         * NOTE: does absolute nothing if directional light
         */
        public abstract void setPosition(float x, float y);

        /**
         * set light starting position
         * 
         * NOTE: does absolute nothing if directional light
         */
        public abstract void setPosition(Vector2 position);

        final Vector2 tmpPosition = new Vector2();

        /**
         * starting position of light in world coordinates. directional light return
         * zero vector.
         * 
         * NOTE: changing this vector does nothing
         * 
         * @return posX
         */
        public Vector2 getPosition() {
                return tmpPosition;
        }

        /**
         * horizontal starting position of light in world coordinates. directional
         * light return 0
         */
        /**
         * @return posX
         */
        public abstract float getX();

        /**
         * vertical starting position of light in world coordinates. directional
         * light return 0
         */
        /**
         * @return posY
         */
        public abstract float getY();

        void staticUpdate() {
                boolean tmp = rayHandler.culling;
                staticLight = !staticLight;
                rayHandler.culling = false;
                update();
                rayHandler.culling = tmp;
                staticLight = !staticLight;
        }

        public final boolean isActive() {
                return active;
        }

        /**
         * disable/enables this light updates and rendering.
         * 
         * @param active
         */
        public final void setActive(boolean active) {
                if (active == this.active)
                        return;

                if (active) {
                        rayHandler.lightList.add(this);
                        rayHandler.disabledLights.removeValue(this, true);
                } else {
                        rayHandler.disabledLights.add(this);
                        rayHandler.lightList.removeValue(this, true);

                }

                this.active = active;

        }

        /**
         * do light beams go through obstacles
         * 
         * @return
         */
        public final boolean isXray() {
                return xray;
        }

        /**
         * disable/enables xray beams. enabling this will allow beams go through
         * obstacles this reduce cpu burden of light about 70%. Use combination of
         * xray and non xray lights wisely
         * 
         * @param xray
         */
        public final void setXray(boolean xray) {
                this.xray = xray;
                if (staticLight)
                        staticUpdate();
        }

        /**
         * return is this light static. Static light do not get any automatic
         * updates but setting any parameters will update it. Static lights are
         * usefull for lights that you want to collide with static geometry but
         * ignore all the dynamic objects.
         * 
         * @return
         */
        public final boolean isStaticLight() {
                return staticLight;
        }

        /**
         * disables/enables staticness for light. Static light do not get any
         * automatic updates but setting any parameters will update it. Static
         * lights are usefull for lights that you want to collide with static
         * geometry but ignore all the dynamic objects. Reduce cpu burden of light
         * about 90%.
         * 
         * @param staticLight
         */
        public final void setStaticLight(boolean staticLight) {
                this.staticLight = staticLight;
                if (staticLight)
                        staticUpdate();
        }

        /**
         * is tips of light beams soft
         * 
         * @return
         */
        public final boolean isSoft() {
                return soft;
        }

        /**
         * disable/enables softness on tips of lights beams.
         * 
         * @param soft
         */
        public final void setSoft(boolean soft) {
                this.soft = soft;
                if (staticLight)
                        staticUpdate();
        }

        /**
         * return how much is softness used in tip of the beams. default 2.5
         * 
         * @return
         */
        public final float getSoftShadowLenght() {
                return softShadowLenght;
        }

        /**
         * set how much is softness used in tip of the beams. default 2.5
         * 
         * @param softShadowLenght
         */
        public final void setSoftnessLenght(float softShadowLenght) {
                this.softShadowLenght = softShadowLenght;
                if (staticLight)
                        staticUpdate();
        }

        private final void setRayNum(int rays) {

                if (rays < MIN_RAYS)
                        rays = MIN_RAYS;

                rayNum = rays;
                vertexNum = rays + 1;

                segments = new float[vertexNum * 8];
                mx = new float[vertexNum];
                my = new float[vertexNum];
                f = new float[vertexNum];

        }

        static final float zero = Color.toFloatBits(0f, 0f, 0f, 0f);

        /**
         * Color getColor
         * 
         * @return current lights color
         */
        public Color getColor() {
                return color;
        }

        /**
         * float getDistance()
         * 
         * @return light rays distance.
         */
        public float getDistance() {
                float dist = distance / RayHandler.gammaCorrectionParameter;
                return dist;
        }

        /** method for checking is given point inside of this light */
        public boolean contains(float x, float y) {
                return false;
        }

        final RayCastCallback ray = new RayCastCallback() {
                @Override
                final public float reportRayFixture(Fixture fixture, Vector2 point,
                                Vector2 normal, float fraction) {

                        if ((filterA != null) && !contactFilter(fixture))
                                return -1;
                        // if (fixture.isSensor())
                        // return -1;
                        mx[m_index] = point.x;
                        my[m_index] = point.y;
                        f[m_index] = fraction;
                        return fraction;
                }
        };

        final boolean contactFilter(Fixture fixtureB) {
                Filter filterB = fixtureB.getFilterData();

                if (filterA.groupIndex == filterB.groupIndex && filterA.groupIndex != 0)
                        return filterA.groupIndex > 0;

                return (filterA.maskBits & filterB.categoryBits) != 0
                                && (filterA.categoryBits & filterB.maskBits) != 0;

        }

        /** light filter **/
        static private Filter filterA = null;

        /**
         * set given contact filter for ALL LIGHTS
         * 
         * @param filter
         */
        static public void setContactFilter(Filter filter) {
                filterA = filter;
        }

        /**
         * create new contact filter for ALL LIGHTS with give parameters
         * 
         * @param categoryBits
         * @param groupIndex
         * @param maskBits
         */
        static public void setContactFilter(short categoryBits, short groupIndex,
                        short maskBits) {
                filterA = new Filter();
                filterA.categoryBits = categoryBits;
                filterA.groupIndex = groupIndex;
                filterA.maskBits = maskBits;
        }

}