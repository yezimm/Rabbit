package com.yesco.rabbit.feature.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;

import com.yesco.rabbit.R;

import java.util.HashMap;
import java.util.Vector;

public class RabbitSurfaceView extends SurfaceView implements Callback, Runnable, View.OnTouchListener {

    private Context context = null;

    // 用于控制SurfaceView
    private SurfaceHolder sfh = null;
    // 声明一个画笔
    private Paint paint;
    // 用于控制线程的标识符
    private boolean flag;
    // 声明一个画布
    private Canvas canvas;
    // 定义高和宽
    public static int screenW, screenH;

    // 行数
    private static final int ROW = 9;
    // 列数
    private static final int COL = 9;
    // 障碍的数量
    private static final int BOCKS = COL * ROW / 5;
    // 每个通道的宽度
    private int WIDTH;
    // 奇数行和偶数行通道间的位置偏差量
    private int DISTANCE;
    // 屏幕顶端和通道最顶端间的距离
    private int OFFSET;
    // 整个通道与屏幕两端间的距离
    private int length;
    // 做成神经猫动态图效果的单张图片
    private Drawable cat_drawable;
    // 背景图
    private Drawable background;
    // 神经猫动态图的索引
    private int index = 0;

    private Point[][] matrix;

    private Point cat;

    //行走的步数
    private int steps;

    private boolean canMove = true;

    private int[] images = {R.drawable.rabbit1, R.drawable.rabbit2, R.drawable.rabbit3,
            R.drawable.rabbit4, R.drawable.rabbit5, R.drawable.rabbit6, R.drawable.rabbit7,
            R.drawable.rabbit8, R.drawable.rabbit9, R.drawable.rabbit10,
            R.drawable.rabbit11, R.drawable.rabbit12, R.drawable.rabbit13,
            R.drawable.rabbit14, R.drawable.rabbit15, R.drawable.rabbit16};

    public RabbitSurfaceView(Context context) {
        super(context);
        this.context = context;
        // ///////////SurfaceView框架/////////////////////////////
        sfh = (SurfaceHolder) this.getHolder();
        sfh.addCallback(this);
        canvas = new Canvas();
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);

        matrix = new Point[ROW][COL];

        if (Build.VERSION.SDK_INT < 21) {
            cat_drawable = ContextCompat.getDrawable(context, images[index]);
            background = ContextCompat.getDrawable(context, R.mipmap.game_bg);
        } else {
            cat_drawable = getResources().getDrawable(images[index], null);
            background = getResources().getDrawable(R.mipmap.game_bg, null);
        }
        initGame();
        setOnTouchListener(this);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
    }

    // 初始化游戏
    private void initGame() {
        steps = 0;
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                matrix[i][j] = new Point(j, i);
            }
        }
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                matrix[i][j].setStatus(Point.STATUS.STATUS_OFF);
            }
        }
        cat = new Point(COL / 2 - 1, ROW / 2 - 1);
        getDot(cat.getX(), cat.getY()).setStatus(Point.STATUS.STATUS_IN);
        for (int i = 0; i < BOCKS; ) {
            int x = (int) ((Math.random() * 100) % COL);
            int y = (int) ((Math.random() * 100) % ROW);
            if (getDot(x, y).getStatus() == Point.STATUS.STATUS_OFF) {
                getDot(x, y).setStatus(Point.STATUS.STATUS_ON);
                i++;
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        screenW = this.getWidth();
        screenH = this.getHeight();
        flag = true;
        // 声明一条线程
        Thread th = new Thread(this);
        th.start();
    }

    /**
     * 绘制画面
     */
    public void myDraw() {
        try {
            canvas = sfh.lockCanvas();
            background.setBounds(0, 0, screenW, screenH);
            background.draw(canvas);
            canvas.save();
            paint.setColor(getResources().getColor(R.color.bg_canvas));
            canvas.drawRect(new RectF(0, OFFSET - WIDTH / 2, screenW, OFFSET + WIDTH * ROW + WIDTH / 2), paint);
            for (int i = 0; i < ROW; i++) {
                for (int j = 0; j < COL; j++) {
                    DISTANCE = 0;
                    if (i % 2 != 0) {
                        DISTANCE = WIDTH / 2;
                    }
                    Point dot = getDot(j, i);
                    switch (dot.getStatus()) {
                        case STATUS_IN:
                            paint.setColor(getResources().getColor(R.color.in));
                            break;
                        case STATUS_ON:
                            paint.setColor(getResources().getColor(R.color.on));
                            break;
                        case STATUS_OFF:
                            paint.setColor(getResources().getColor(R.color.off));
                            break;
                        default:
                            break;
                    }
                    canvas.drawOval(new RectF(dot.getX() * WIDTH + DISTANCE
                            + length, dot.getY() * WIDTH + OFFSET, (dot.getX() + 1)
                            * WIDTH + DISTANCE + length, (dot.getY() + 1) * WIDTH
                            + OFFSET), paint);
                }
            }
            paint.setColor(getResources().getColor(R.color.white));
            canvas.drawLine(0, OFFSET - WIDTH / 2, screenW, OFFSET - WIDTH / 2, paint);
            canvas.drawLine(0, OFFSET + WIDTH * ROW + WIDTH / 2, screenW, OFFSET + WIDTH * ROW + WIDTH / 2, paint);
            int left;
            int top;
            if (cat.getY() % 2 == 0) {
                left = cat.getX() * WIDTH;
                top = cat.getY() * WIDTH;
            } else {
                left = (WIDTH / 2) + cat.getX() * WIDTH;
                top = cat.getY() * WIDTH;
            }
            // 此处神经猫图片的位置是根据效果图来调整的
            cat_drawable.setBounds(left - WIDTH / 6 + length, top - WIDTH / 2
                    + OFFSET, left + WIDTH + length, top + WIDTH + OFFSET);
            cat_drawable.draw(canvas);
            canvas.restore();
        } catch (Exception e) {

        } finally {
            if (canvas != null) {
                sfh.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * 页面逻辑
     */
    public void logic() {
        index++;
        if (index > images.length - 1) {
            index = 0;
        }
        if (Build.VERSION.SDK_INT < 21) {
            cat_drawable = ContextCompat.getDrawable(context, images[index]);
        } else {
            cat_drawable = getResources().getDrawable(images[index], null);
        }
    }

    @Override
    public void run() {
        while (flag) {
            long start = System.currentTimeMillis();
            myDraw();
            logic();
            long end = System.currentTimeMillis();
            try {
                if (end - start < 50) {
                    Thread.sleep(50 - (end - start));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 获取通道对象
    private Point getDot(int x, int y) {
        return matrix[y][x];
    }

    // 判断神经猫是否处于边界
    private boolean inEdge(Point dot) {
        return dot.getX() * dot.getY() == 0 || dot.getX() + 1 == COL || dot.getY() + 1 == ROW;
    }

    // 移动cat至指定点
    private void moveTo(Point dot) {
        dot.setStatus(Point.STATUS.STATUS_IN);
        getDot(cat.getX(), cat.getY()).setStatus(Point.STATUS.STATUS_OFF);
        cat.setXY(dot.getX(), dot.getY());
    }

    // 获取one在方向dir上的可移动距离
    private int getDistance(Point one, int dir) {
        int distance = 0;
        if (inEdge(one)) {
            return 1;
        }
        Point ori = one;
        Point next;
        while (true) {
            next = getNeighbour(ori, dir);
            if (next != null && next.getStatus() == Point.STATUS.STATUS_ON) {
                return distance * -1;
            }
            if (inEdge(next)) {
                distance++;
                return distance;
            }
            distance++;
            ori = next;
        }
    }

    // 获取dot的相邻点，返回其对象
    private Point getNeighbour(Point dot, int dir) {
        switch (dir) {
            case 1:
                return getDot(dot.getX() - 1, dot.getY());
            case 2:
                if (dot.getY() % 2 == 0) {
                    return getDot(dot.getX() - 1, dot.getY() - 1);
                } else {
                    return getDot(dot.getX(), dot.getY() - 1);
                }
            case 3:
                if (dot.getY() % 2 == 0) {
                    return getDot(dot.getX(), dot.getY() - 1);
                } else {
                    return getDot(dot.getX() + 1, dot.getY() - 1);
                }
            case 4:
                return getDot(dot.getX() + 1, dot.getY());
            case 5:
                if (dot.getY() % 2 == 0) {
                    return getDot(dot.getX(), dot.getY() + 1);
                } else {
                    return getDot(dot.getX() + 1, dot.getY() + 1);
                }
            case 6:
                if (dot.getY() % 2 == 0) {
                    return getDot(dot.getX() - 1, dot.getY() + 1);
                } else {
                    return getDot(dot.getX(), dot.getY() + 1);
                }
        }
        return null;
    }

    // cat的移动算法
    private void move() {
        if (inEdge(cat)) {
            failure();
            return;
        }
        Vector<Point> available = new Vector<>();
        Vector<Point> direct = new Vector<>();
        HashMap<Point, Integer> hash = new HashMap<>();
        for (int i = 1; i < 7; i++) {
            Point n = getNeighbour(cat, i);
            if (n != null && n.getStatus() == Point.STATUS.STATUS_OFF) {
                available.add(n);
                hash.put(n, i);
                if (getDistance(n, i) > 0) {
                    direct.add(n);
                }
            }
        }
        if (available.size() == 0) {
            win();
            canMove = false;
        } else if (available.size() == 1) {
            moveTo(available.get(0));
        } else {
            Point best = null;
            if (direct.size() != 0) {
                int min = 20;
                for (int i = 0; i < direct.size(); i++) {
                    if (inEdge(direct.get(i))) {
                        best = direct.get(i);
                        break;
                    } else {
                        int t = getDistance(direct.get(i),
                                hash.get(direct.get(i)));
                        if (t < min) {
                            min = t;
                            best = direct.get(i);
                        }
                    }
                }
            } else {
                int max = 1;
                for (int i = 0; i < available.size(); i++) {
                    int k = getDistance(available.get(i),
                            hash.get(available.get(i)));
                    if (k < max) {
                        max = k;
                        best = available.get(i);
                    }
                }
            }
            moveTo(best);
        }
        if (inEdge(cat)) {
            failure();
        }
    }

    // 通关失败
    private void failure() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(getResources().getString(R.string.title_failure));
        dialog.setMessage("");
        dialog.setCancelable(false);
        dialog.setNegativeButton(getResources().getString(R.string.restart), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                initGame();
                canMove = true;
            }
        });
        dialog.setPositiveButton(getResources().getString(R.string.cancel), null);
        dialog.show();
    }

    // 通关成功
    private void win() {
        String testStr = getResources().getString(R.string.tips_win);
        String message = String.format(testStr, steps + 1);
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(getResources().getString(R.string.title_win));
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setNegativeButton(getResources().getString(R.string.restart), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                initGame();
                canMove = true;
            }
        });
        dialog.setPositiveButton(getResources().getString(R.string.cancel), null);
        dialog.show();
    }

    // 触屏事件
    public boolean onTouch(View v, MotionEvent event) {

        int x, y;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (event.getY() <= OFFSET) {
                return true;
            }
            y = (int) ((event.getY() - OFFSET) / WIDTH);
            if (y % 2 == 0) {
                if (event.getX() <= length
                        || event.getX() >= length + WIDTH * COL) {
                    return true;
                }
                x = (int) ((event.getX() - length) / WIDTH);
            } else {
                if (event.getX() <= (length + WIDTH / 2)
                        || event.getX() > (length + WIDTH / 2 + WIDTH * COL)) {
                    return true;
                }
                x = (int) ((event.getX() - WIDTH / 2 - length) / WIDTH);
            }
            if (x + 1 > COL || y + 1 > ROW) {
                return true;
            } else if (inEdge(cat) || !canMove) {
                initGame();
                canMove = true;
                return true;
            } else if (getDot(x, y).getStatus() == Point.STATUS.STATUS_OFF) {
                getDot(x, y).setStatus(Point.STATUS.STATUS_ON);
                move();
                steps++;
            }
        }
        return true;
    }

    // 按键事件
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            flag = false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 画布状态改变监听事件
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        WIDTH = width / (COL + 1);
        OFFSET = (height - WIDTH * ROW) / 2;
        length = WIDTH / 3;
        screenW = width;
        screenH = height;
    }

    /**
     * 画布被摧毁事件
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
    }

}

