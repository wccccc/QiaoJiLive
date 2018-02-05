package xin.heipichao.qiaojilive.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import xin.heipichao.qiaojilive.R;

/**
 * Created by Chaochao.Wen on 2018/2/5.
 */

public class SelectView extends View{
    private Paint mPaint;
    private Drawable mThumb;
    private float mPosition;
    private String mLeftText;
    private String mRightText;
    private int mLeftTextColor;
    private int mRightTextColor;
    private float mTextSize;
    private boolean mSelected;
    private OnSelectChangeListener mOnSelectChangeListener;
    public SelectView(Context context) {
        this(context,null,0);
    }

    public SelectView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SelectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray=context.obtainStyledAttributes(attrs, R.styleable.SelectView);
        mThumb=typedArray.getDrawable(R.styleable.SelectView_thumb);
        mLeftText=typedArray.getString(R.styleable.SelectView_leftText);
        mRightText=typedArray.getString(R.styleable.SelectView_rightText);
        mLeftTextColor=typedArray.getColor(R.styleable.SelectView_leftTextColor,Color.WHITE);
        mRightTextColor=typedArray.getColor(R.styleable.SelectView_rightTextColor,0xFFFD4C5C);
        mTextSize =typedArray.getDimension(R.styleable.SelectView_textSize,24);
        mPaint=new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        int height = canvas.getHeight();
        int width = canvas.getWidth();
        super.onDraw(canvas);
        if(getBackground()==null){
            mPaint.setColor(0xFFFFFFFF);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.drawRoundRect(0,0,width,height,height/2F,height/2F,mPaint);
            }else{
                RectF bg = new RectF(0,0,width,height);
                canvas.drawRoundRect(bg,height/2F,height/2F,mPaint);
            }
        }

        int thumbTop=0;
        int thumbBottom=height;
        int thumbLeft = (int) ((width/2F)*mPosition);
        int thumbRight = (int) (thumbLeft+width/2F);
        if(mThumb!=null){
            mThumb.setBounds(thumbLeft,thumbTop,thumbRight,thumbBottom);
            mThumb.draw(canvas);
        }else{
            drawDefaultThumb(thumbLeft,thumbTop,thumbRight,thumbBottom,canvas);
        }

        //文字
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(colorGradient(mPosition));
        drawText(mLeftText,width/4F,canvas);
        mPaint.setColor(colorGradient(1-mPosition));
        drawText(mRightText,(width/4F)*3F,canvas);
    }

    private void drawText(String text,float x,Canvas canvas){
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float top = fontMetrics.top;
        float bottom = fontMetrics.bottom;
        int baseLineY = (int) (canvas.getHeight()/2F - top/2 - bottom/2);
        canvas.drawText(text,x,baseLineY,mPaint);
    }

    private int colorGradient(float position){
        int red= (int) ((Color.red(mRightTextColor)-Color.red(mLeftTextColor))*position);
        int green= (int) ((Color.green(mRightTextColor)-Color.green(mLeftTextColor))*position);
        int blue= (int) ((Color.blue(mRightTextColor)-Color.blue(mLeftTextColor))*position);
        int color=Color.rgb(Color.red(mLeftTextColor)+red,Color.green(mLeftTextColor)+green,Color.blue(mLeftTextColor)+blue);
        return color;
    }

    public void setPosition(float position) {
        this.mPosition = position;
    }

    private void drawDefaultThumb(int left, int top, int right, int bottom, Canvas canvas){
        top+=2;
        bottom-=2;
        left+=2;
        right-=2;
        int height=bottom-top;
        mPaint.setColor(getContext().getResources().getColor(R.color.mainColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(left,top,right,bottom,height/2F,height/2F,mPaint);
        }else{
            RectF bg = new RectF(left,top,right,bottom);
            canvas.drawRoundRect(bg,height/2F,height/2F,mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_UP){
            float x=event.getRawX();
            if(x<getWidth()/2F&&mSelected){
                setSelected(false);
                if(mOnSelectChangeListener!=null){
                    mOnSelectChangeListener.onSelectChange(this,isSelected());
                }
                return true;
            }else if(x>=getWidth()/2F&&!mSelected){
                setSelected(true);
                if(mOnSelectChangeListener!=null){
                    mOnSelectChangeListener.onSelectChange(this,isSelected());
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    public void setOnSelectChangeListener(OnSelectChangeListener onSelectChangeListener) {
        this.mOnSelectChangeListener = onSelectChangeListener;
    }

    public interface OnSelectChangeListener{
        void onSelectChange(SelectView view,boolean selected);
    }
}
