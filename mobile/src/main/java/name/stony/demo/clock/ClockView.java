package name.stony.demo.clock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * TODO: document your custom view class.
 */
public class ClockView extends View {

    public interface ClockViewListener {
        void onceADay(ClockView view);
        void onceAnHour(ClockView view);
    }

    private static final float SECOND_UP_RATIO = 1.0f;
    private static final float SECOND_DOWN_RATIO = 0.9f;

    private static final float MINITE_LENGTH_RATIO = 0.7f;
    private static final float HOUR_LENGTH_RATIO = 0.5f;

    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private GregorianCalendar mCalendar;
    private android.location.Location mLocation;
    private Calendar mSunrise;
    private Calendar mSunset;
    private ClockViewListener mListener;

    private DateFormat mFormatter;
    private int mDateOfYear;
    private int mCachedHourOfDay;

    private Paint mDayPaint;
    private Paint mNightPaint;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    public ClockView(Context context) {
        super(context);
        init(null, 0);
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ClockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ClockView, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.ClockView_exampleString);
        mExampleColor = a.getColor(
                R.styleable.ClockView_exampleColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.ClockView_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.ClockView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.ClockView_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mDayPaint = new Paint();
        mDayPaint.setColor(Color.BLACK);

        mNightPaint = new Paint();
        mNightPaint.setColor(Color.WHITE);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();

        mCalendar = new GregorianCalendar();
        mFormatter = new SimpleDateFormat("hh:mm:ss");
        resetDateOfYear();
        resetHourOfDay();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        int circleD = Math.min(contentHeight, contentWidth);
        float centerX = paddingLeft + contentWidth / 2.0f;
        float centerY = paddingTop + contentHeight / 2.0f;
        float radius = circleD / 2.0f;

//        // Draw the text.
//        canvas.drawText(mFormatter.format(mCalendar.getTime()),
//                paddingLeft + (contentWidth - mTextWidth) / 2,
//                paddingTop + (contentHeight + mTextHeight) / 2,
//                mTextPaint);
//
//        // Draw the example drawable on top of the text.
//        if (mExampleDrawable != null) {
//            mExampleDrawable.setBounds(paddingLeft, paddingTop,
//                    paddingLeft + contentWidth, paddingTop + contentHeight);
//            mExampleDrawable.draw(canvas);
//        }

        if (mCalendar != null) {

            int dateOfYear = mCalendar.get(Calendar.DAY_OF_YEAR);
            if (dateOfYear != mDateOfYear) {
                cacheDateOfYear(dateOfYear);
                if (mListener != null) {
                    mListener.onceADay(this);
                }
//                reloadSunrise(mCalendar);
            }

            int hOfDay = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hOfDay != mCachedHourOfDay) {
                cacheHourOfDay(hOfDay);
                if (mListener != null) {
                    mListener.onceAnHour(this);
                }
            }

            int h = mCalendar.get(Calendar.HOUR);
            int m = mCalendar.get(Calendar.MINUTE);
            int s = mCalendar.get(Calendar.SECOND);
            int ss = (s == 0)? 60 : s;

            Paint paint;
            if (mCalendar.after(mSunrise) && mCalendar.before(mSunset)) {
                paint = mDayPaint;
            } else {
                paint = mNightPaint;
            }

            drawHourMinutePan(h, m, s, centerX, centerY, radius, canvas, paint);

            int barCount = ss / 5;
            for (int i = 0; i < barCount; i++) {
                drawSeconds(i * 5, i * 5 + 5, centerX, centerY, radius, canvas, paint);
            }
            if (ss > barCount * 5) {
                drawSeconds(barCount * 5, ss, centerX, centerY, radius, canvas, paint);
            }
        }
    }

    private void drawHourMinute(int hour, int minute, float centerX, float centerY, float radius, Canvas canvas, Paint paint) {

        int fromDeg = hour * 30 + minute / 2;
        int toDeg = minute * 6;
        float controlDeg;
        if (toDeg == fromDeg) {
            controlDeg = fromDeg;
        } else if (toDeg < fromDeg) {
            controlDeg = (fromDeg + toDeg + 360) / 2.0f;
        } else {
            controlDeg = (fromDeg + toDeg) / 2.0f;
        }

        float pointAX = centerX + (float) (radius * HOUR_LENGTH_RATIO * Math.sin((fromDeg) * Math.PI / 180.0f));
        float pointAY = centerY - (float) (radius * HOUR_LENGTH_RATIO * Math.cos((fromDeg) * Math.PI / 180.0f));

        float pointCX = centerX + (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) / 2 * Math.sin((controlDeg) * Math.PI / 180.0f));
        float pointCY = centerY - (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) / 2 * Math.cos((controlDeg) * Math.PI / 180.0f));

        float pointEX = centerX + (float) (radius * MINITE_LENGTH_RATIO * Math.sin((toDeg) * Math.PI / 180.0f));
        float pointEY = centerY - (float) (radius * MINITE_LENGTH_RATIO * Math.cos((toDeg) * Math.PI / 180.0f));

        Path path = new Path();
        path.setFillType(Path.FillType.WINDING);

        path.moveTo(centerX, centerY);
        path.lineTo(pointAX, pointAY);
        if (toDeg < fromDeg ) {

            float pointBX = centerX + (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) * Math.sin(((toDeg + 360) * 1 + fromDeg * 2) / 3 * Math.PI / 180.0f));
            float pointBY = centerY - (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) * Math.cos(((toDeg + 360) * 1 + fromDeg * 2) / 3 * Math.PI / 180.0f));

            float pointDX = centerX + (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) * 1.3 * Math.sin(((toDeg + 360) * 2 + fromDeg * 1) / 3 * Math.PI / 180.0f));
            float pointDY = centerY - (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) * 1.3 * Math.cos(((toDeg + 360) * 2 + fromDeg * 1) / 3 * Math.PI / 180.0f));
//
//            path.quadTo(pointBX, pointBY, pointCX, pointCY);
//            path.quadTo(pointDX, pointDY, pointEX, pointEY);

//            pointCX = centerX + (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) * Math.sin((controlDeg) * Math.PI / 180.0f));
//            pointCY = centerY - (float) (radius * (HOUR_LENGTH_RATIO + MINITE_LENGTH_RATIO) * Math.cos((controlDeg) * Math.PI / 180.0f));

            path.cubicTo(pointBX, pointBY, pointDX, pointDY, pointEX, pointEY);
        } else {
            path.quadTo(pointCX, pointCY, pointEX, pointEY);
        }
        path.lineTo(centerX, centerY);
        canvas.drawPath(path, paint);
    }

    private void drawHourMinutePan(int hour, int minute, int second, float centerX, float centerY, float radius, Canvas canvas, Paint paint) {
        int fromDeg = hour * 30 + minute / 2 - 90;
        int toDeg = (minute * 6 + second / 10 + 360 - hour * 30 - minute / 2) % 360;
        if (toDeg == 0) {
            toDeg = 1;
        }
        canvas.drawArc(centerX - radius * MINITE_LENGTH_RATIO, centerY - radius * MINITE_LENGTH_RATIO,
                centerX + radius * MINITE_LENGTH_RATIO, centerY + radius * MINITE_LENGTH_RATIO,
                fromDeg, toDeg,
                true, paint);
    }

    private void drawSeconds(int fromSec, int toSec, float centerX, float centerY, float radius, Canvas canvas, Paint paint) {
        Path path = new Path();
        path.setFillType(Path.FillType.WINDING);

        int fromDeg = fromSec * 6 + 1;
        int toDeg = toSec * 6;
        if (toSec % 5 == 0) {
            toDeg = toDeg - 1;
        }

        float pointAX = centerX + (float) (radius * SECOND_DOWN_RATIO * Math.sin((fromDeg) * Math.PI / 180.0f));
        float pointAY = centerY - (float) (radius * SECOND_DOWN_RATIO * Math.cos((fromDeg) * Math.PI / 180.0f));

        float pointBX = centerX + (float) (radius * SECOND_UP_RATIO * Math.sin((fromDeg) * Math.PI / 180.0f));
        float pointBY = centerY - (float) (radius * SECOND_UP_RATIO * Math.cos((fromDeg) * Math.PI / 180.0f));

        float pointCX = centerX + (float) (radius * SECOND_UP_RATIO * Math.sin((toDeg) * Math.PI / 180.0f));
        float pointCY = centerY - (float) (radius * SECOND_UP_RATIO * Math.cos((toDeg) * Math.PI / 180.0f));

        float pointDX = centerX + (float) (radius * SECOND_DOWN_RATIO * Math.sin((toDeg) * Math.PI / 180.0f));
        float pointDY = centerY - (float) (radius * SECOND_DOWN_RATIO * Math.cos((toDeg) * Math.PI / 180.0f));

        path.moveTo(pointAX, pointAY);
        path.lineTo(pointBX, pointBY);
        path.arcTo(centerX - radius * SECOND_UP_RATIO, centerY - radius * SECOND_UP_RATIO,
                centerX + radius * SECOND_UP_RATIO, centerY + radius * SECOND_UP_RATIO,
                fromDeg - 90, (toDeg - fromDeg), true);
        path.lineTo(pointAX, pointAY);
        path.arcTo(centerX - radius * SECOND_DOWN_RATIO, centerY - radius * SECOND_DOWN_RATIO,
                centerX + radius * SECOND_DOWN_RATIO, centerY + radius * SECOND_DOWN_RATIO,
                fromDeg - 90, (toDeg - fromDeg), true);
        path.lineTo(pointCX, pointCY);

        canvas.drawPath(path, paint);
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }


    @NonNull
    public Calendar getSunriseCalendar() {
        return mSunrise;
    }

    public void setSunriseCalendar(@NonNull Calendar sunrise) {
        mSunrise = sunrise;
    }

    public void setSunriseInMillis(long milliseconds) {
        mSunrise.setTimeInMillis(milliseconds);
    }

    @NonNull
    public Calendar getSunsetCalendar() {
        return mSunset;
    }

    public void setSunsetCalendar(@NonNull Calendar sunset) {
        mSunset = sunset;
    }

    public void setSunsetInMillis(long milliseconds) {
        mSunset.setTimeInMillis(milliseconds);
    }

    @NonNull
    public Calendar getCalendar() {
        return mCalendar;
    }

    public long getTimeInMillis() {
        return mCalendar.getTimeInMillis();
    }

    public void setTimeInMillis(long milliseconds) {
        mCalendar.setTimeInMillis(milliseconds);
        invalidate();
    }

    public void setTimeZone(TimeZone timeZone) {
        mCalendar.setTimeZone(timeZone);
    }

    public android.location.Location getLocation() {
        return mLocation;
    }

    public void setLocation(@NonNull android.location.Location location) {
        mLocation = location;
        resetDateOfYear();
    }

    public void setListener(ClockViewListener listener) {
        mListener = listener;
    }

    public void resetDefaulSunRiseAndSunSet() {
        Calendar sunrise = (Calendar) mCalendar.clone();
        sunrise.set(Calendar.HOUR_OF_DAY, 6);
        sunrise.set(Calendar.MINUTE, 0);
        sunrise.set(Calendar.SECOND, 0);
        mSunrise = sunrise;
        Calendar sunset = (Calendar) sunrise.clone();
        sunset.set(Calendar.HOUR_OF_DAY, 18);
        mSunset = sunset;
    }

    public void resetHourOfDay() {
        mCachedHourOfDay = -1;
    }

    public void resetDateOfYear() {
        mDateOfYear = 0;
    }

    private void cacheDateOfYear(int dateOfYear) {
        mDateOfYear = dateOfYear;
    }

    private void cacheHourOfDay(int hourOfDay) {
        mCachedHourOfDay = hourOfDay;
    }
}
