package inputdisplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

class InputDisplayFrame extends JFrame {
    
    private KeyInfo[] keyArr = new KeyInfo[256];
    
    private static final int paintFreq = 33; //144fps = 7, 60fps = 17, 30fps = 33
    
    private static int framewidth = 0; //Required Frame Width to fit all elements
    private static int frameheight = 0; //Required Frame Height to fit all elements
    
    private static Color backgroundColor = new Color(0,255,0,255);
    private BufferedImage imgBuffer;
    private Graphics2D imgBufferG;
    
    //KEYBOARD SECTION
    private static final int keyWidth = 40; //Width of individual keys
    private static final int keyHeight = 40; //Height of individual keys
    private static final int keyBorderSize = 5; //Size of border when key is not pressed
    private static final int keyBorderSizeOn = 2; //Size of border when key is pressed
    private static final int keyPadding = 2; //Gap between keys
    private static final int horizontalKeyStagger = 20; //Gaps between main keys, arrow keys, and numpad sections
    private static final int verticalKeyStagger = 30; //Gap between main key section and F(function) key section
    
    private static final Color keyOnColor = new Color(255,255,255,255);
    private static final Color keyOffColor = new Color(0,0,0,255);
    
    private static final int keyFontSize = 14; //Font size of key text
    private static final boolean keyUseFadeout = true;
    private static final int keyFadeoutHangTime = 1000; //How long before beginning to fadeout
    private static final int keyFadeoutTime = 2000; //In milliseconds
    
    private int keyboardX = 0;
    private int keyboardY = 0;
    private int keyboardWidth = 0; //Stores total width of keyboard
    private int keyboardHeight = 0; //Stores total height of keyboard
    //
    
    //MOUSE SECTION
    //Mouse Grid
    private static final boolean mouseDrawGrid = false; //Whether to draw grid
    private static final int mouseGridGap = 100; //Size of grid squares
    private static final int mouseGridWidth = 5;
    private static final double mouseGridScale = 8.0; //Proprtion to increase virtual grid size
    //Mouse Border
    private static final boolean mouseDrawBorder = false; //Whether to draw border
    private static final int mouseBorderWidth = 7;
    //Mouse Buttons
    private static final int mouseWheelPadding = 2; //Size reduction of mouse wheel direction indicators
    private static final int mouseWheelFadeout = 500; //Milliseconds to fade out a mousewheel after usage
    private static final int mouseButtonHeight = keyHeight; //Height of the mouse buttons
    private static final int mouseMiddleButtonWidth = keyWidth/2; //Width of the middle mouse button
    private static final int mouseButtonGap = 5; //Padding between mouse buttons
    

    private static final Color mouseWheelColor = new Color(255,255,255,255); //Color for when a mouse wheel is used
    private static final Color mouseButtonOnColor = new Color(255,255,255,255);
    private static final Color mouseButtonOffColor = new Color(0,0,0,80);
    //Mouse Ball
    private static final int mouseBallSize = 25; //Total radius of ball
    private static final int mouseBallBorderSize = 5; //Radius of Ball's border
    private static final int mouseBallThreadSize = 3;
    private static final int mouseBallThreadBorderSize = 7;
    private static final double mouseBallVelMul = 2.0; //Pixels per velocity of mouse for ball to move
    private static final int mouseVelUpdateRate = 1; //Ticks between velocity calculations
    
    private static boolean mouseGameMode = false;
    private static boolean mouseGameModeAutodetect = true; //Automatically detect game mode
    private static final int mouseGameModeAutodetectSamples = 200; //Amount of recent mouse positions to consider
    private static final int mouseGameModeAutodetectSensOn = 50; //Maximum distance traveled in samples to activate game mode
    private static final int mouseGameModeAutodetectSensOff = 200; //Minimum distance traveled in samples to deactivate game mode
    private static final int mouseGameModeAutodetectLocPredThresh = 500; //Maximum distance from the mouse anchor point to consider for game mode
    private static final int mouseGameModeAutodetectGraceTicks = 100; //How many checks must be failed to change the mode
    private static double mouseGameModeVelMul = 0.25; //Ball Vel Mul during game mode
    private static int mouseGameModeX = 960;
    private static int mouseGameModeY = 540;
    
    private static final Color mouseMajorColor = new Color(0,0,0,255); //Color for the grid and Ball
    private static final Color mouseMinorColor = new Color(255,255,255,255); //Color for the Ball Border
    
    private int mouseX = 0;
    private int mouseY = 0;
    private int mouseWidth = 0; //Stores total width of mouse component
    private int mouseHeight = 0; //Stores total height of mouse component
    //
    
    public InputDisplayFrame() {
        setTitle("Input Display");
        
        calcSizes();
        
        setSize(framewidth, frameheight);
        imgBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        imgBufferG = (Graphics2D)imgBuffer.getGraphics();
        
        setupKeyboard(keyboardX,keyboardY);
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        
        //Repaint thread
        new Thread(){
            public void run(){
                while(true){
                    repaint();
                    try{sleep(paintFreq);}catch(Exception e){}
                }
            }
        }.start();
    }
    
    private void calcSizes(){
        //Keyboard
        keyboardWidth = (keyWidth*15+keyPadding*14) + keyPadding*2;
        keyboardHeight = (keyHeight*5+keyPadding*4) + keyPadding*2;
        
        //Mouse
        mouseX = keyboardX+keyboardWidth;
        mouseY = keyboardY;
        mouseWidth = keyboardHeight;
        mouseHeight = keyboardHeight;
        
        //Frame
        framewidth = Math.max(keyboardX+keyboardWidth,mouseX+mouseWidth)+16;
        frameheight = Math.max(keyboardY+keyboardHeight,mouseY+mouseHeight)+39;
        
        //Correct All Positions
        keyboardX+=8;
        keyboardY+=31;
        mouseX+=8;
        mouseY+=31;
    }
    
    public void keyStateChange(int key, boolean state){
        if(key > 255)
            return;
        if(keyArr[key] == null)
            return;
        if(keyArr[key].pressed == true && state == false){
            keyArr[key].lastPressed = System.currentTimeMillis();
        }
        keyArr[key].pressed = state;
    }
    
    
    //Virtual mouse positions
    private int mousePosX = 0;
    private int mousePosY = 0;
    //Actual mouse positions
    private int mouseRealX = 0;
    private int mouseRealY = 0;
    public void mousePosChange(int x, int y){
        mouseRealX = x;
        mouseRealY = y;
        
        if(mouseGameModeAutodetect)
            mouseGMA(x,y);

        if(mouseGameMode){
            mousePosX += x-mouseGameModeX;
            mousePosY += y-mouseGameModeY;
        }else{
            mousePosX = x;
            mousePosY = y;
        }
    }
    
    private boolean leftClickPressed = false;
    private boolean rightClickPressed = false;
    private boolean middleClickPressed = false;
    public void mouseStateChange(int button, boolean state){
        if(button==1)
            leftClickPressed = state;
        else if(button==2)
            rightClickPressed = state;
        else
            middleClickPressed = state;
    }
    
    private long lastWheelUp = 0;
    private long lastWheelDown = 0;
    public void mouseWheelUse(int dir){
        if(dir>0){
            lastWheelUp = System.currentTimeMillis();
        }else{
            lastWheelDown = System.currentTimeMillis();
        }
    }
    
    //Mouse Game Mode Auto Detection
    private int[] mouseGMALX = new int[mouseGameModeAutodetectSamples];
    private int[] mouseGMALY = new int[mouseGameModeAutodetectSamples];
    private int mouseGMARVX = 0; //Running velocity x sum
    private int mouseGMARVY = 0; //Running velocity y sum
    private int mouseGMAFails = 0; //Number of successive failures
    private int mouseGMALPos = 0;
    private void mouseGMA(int x, int y){
        //Remove entries at Pos from the running vel sum
        int ovelx = mouseGMALX[wrapIndx(mouseGMALPos+1,mouseGameModeAutodetectSamples)]-mouseGMALX[mouseGMALPos]; //The x velocity needing to be removed
        mouseGMARVX-=ovelx;
        int ovely = mouseGMALY[wrapIndx(mouseGMALPos+1,mouseGameModeAutodetectSamples)]-mouseGMALY[mouseGMALPos]; //The y velocity needing to be removed
        mouseGMARVY-=ovely;
        //Add new entry to the arrays
        mouseGMALX[mouseGMALPos] = x;
        mouseGMALY[mouseGMALPos] = y;
        //Add to the running vel sum
        int nvelx = x-mouseGMALX[wrapIndx(mouseGMALPos-1,mouseGameModeAutodetectSamples)];
        mouseGMARVX += nvelx;
        int nvely = y-mouseGMALY[wrapIndx(mouseGMALPos-1,mouseGameModeAutodetectSamples)];
        mouseGMARVY += nvely;
        
        int runningvel = Math.abs(mouseGMARVX) + Math.abs(mouseGMARVY);
        //System.out.println("running vel: "+runningvel);
        
        //Update center position for Game Mode
        //mouseGameModeX = x-mouseGMARVX; Not quite accurate yet; maybe use an average
        //mouseGameModeY = y-mouseGMARVY; Not quite accurate yet; maybe use an average
        
        //The prediction based upon running velocity calculations
        boolean velPrediction = mouseGameMode?(runningvel < mouseGameModeAutodetectSensOff):(runningvel < mouseGameModeAutodetectSensOn);
        
        //The prediction based on average cursor location during velocity cycle
        int locPredX = mouseRealX-mouseGMARVX;
        int locPredY = mouseRealY-mouseGMARVY;
        boolean locPrediction = (Math.abs(mouseGameModeX-locPredX)+Math.abs(mouseGameModeY-locPredY))<mouseGameModeAutodetectLocPredThresh;
        
        //Which mode the algorithm believes it should be in
        boolean predictedMode = locPrediction&&velPrediction;
        
        if(predictedMode == mouseGameMode){
            mouseGMAFails = 0; //Reset fail counter
        }else{
            mouseGMAFails++;
            if(mouseGMAFails>mouseGameModeAutodetectGraceTicks){
                //Failed excessive times, change the mode
                mouseGameMode = predictedMode;
                //System.out.println("Changing mode");
                mouseGMAFails = 0;
            }
        }
        
        mouseGMALPos = wrapIndx(mouseGMALPos+1, mouseGameModeAutodetectSamples);
    }
    
    //Wraps an index, indx, around a circular array of size, sz
    private int wrapIndx(int indx, int sz){
        if(indx>=sz)
            return indx-sz;
        if(indx<0)
            return sz+indx;
        return indx;
    }

    private static class KeyInfo {
        
        boolean drawable;
        
        boolean pressed;
        Color onColor;
        Color offColor;
        Color borderColor;
        long lastPressed;
        
        int x, y;
        int w, h;
        String text;
        
        public KeyInfo(){
            drawable = false;
            pressed = false;
            onColor = keyOnColor;
            offColor = keyOffColor;
            lastPressed = 0;
            
            x = 0;
            y = 0;
            w = 0;
            h = 0;
            text = "";
        }
        
        public KeyInfo(String text, int x, int y, int w, int h){
            this();
            
            drawable = true;
            
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
        }
        
    }
    
    //Mouse Think Variables
    private int mouseLastX = 0;
    private int mouseLastY = 0;
    private int mouseVelX = 0;
    private int mouseVelY = 0;
    private int mouseTicksTillUpdate = 0;
    //
    
    //Do necessray things on a frame-by-frame basis
    private void think(){
        //Calculate mouse Velocity
        mouseTicksTillUpdate--;
        if(mouseTicksTillUpdate <= 0){
            mouseVelX = mousePosX - mouseLastX;
            mouseVelY = mousePosY - mouseLastY;
            mouseLastX = mousePosX;
            mouseLastY = mousePosY;
            
            mouseTicksTillUpdate = mouseVelUpdateRate;
        }
        
    }
    
    //Draw a Frame
    public void paint(Graphics g){
        //Do think
        think();
        
        if(imgBuffer.getWidth() != getWidth() || imgBuffer.getHeight() != getHeight()){
            imgBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            imgBufferG = (Graphics2D)imgBuffer.getGraphics();
        }
        
        //Clear the buffer
        imgBufferG.setColor(backgroundColor);
        imgBufferG.fillRect(0, 0, getWidth(), getHeight());
        
        //Draw components
        drawKeyboard(imgBufferG);
        drawMouse(imgBufferG);
        
        //Draw buffer
        g.drawImage(imgBuffer, 0, 0, null);
    }
    
    private void drawKeyboard(Graphics2D g){
        for(KeyInfo ki : keyArr){
            if(ki == null || !ki.drawable)
                continue;
            
          //Draw Key
            double alphaMul = getKeyAlphaMul(ki);
            //Border
            int nBorderSize = keyBorderSize;
            if(ki.pressed)
                nBorderSize = keyBorderSizeOn;
            Color borderCol = getOppKeyCol(ki);
            g.setColor(colMulAlpha(borderCol,alphaMul));
            g.fillRect(ki.x, ki.y, ki.w, ki.h);
                //Complete Border
                g.setColor(backgroundColor);
                g.fillRect(ki.x+nBorderSize, ki.y+nBorderSize, ki.w-nBorderSize*2, ki.h-nBorderSize*2);
            //Inner
            Color innerCol = getKeyCol(ki);
            g.setColor(colMulAlpha(innerCol,alphaMul));
            g.fillRect(ki.x+nBorderSize, ki.y+nBorderSize, ki.w-nBorderSize*2, ki.h-nBorderSize*2);
            //Text
            g.setColor(colMulAlpha(borderCol, alphaMul));
            g.setFont(new Font("TimesRoman", Font.BOLD, keyFontSize));
            g.drawString(ki.text, ki.x+ki.w/4, ki.y+ki.h*3/5);
            
            //System.out.println(""+ki.x+" "+ki.y+" "+ki.w+" "+ki.h);
            
        }
    }
    
    private void drawMouse(Graphics2D g){
        int midx = mouseX+mouseWidth/2;
        int midy = mouseY+mouseHeight/2;
        
        double mul = mouseBallVelMul;
        if(mouseGameMode)
            mul = mouseGameModeVelMul;
        
        double ballmul = mul*7/paintFreq; //Account for paint rate
        ballmul = ballmul*mouseWidth/262d; //Account for window size
        
        //Draw Grid
        if(mouseDrawGrid){
            int gw2 = mouseGridWidth/2;
            int gridmx = -(int)(mousePosX/mouseGridScale*mul)%mouseGridGap;
            int gridmy = -(int)(mousePosY/mouseGridScale*mul)%mouseGridGap;
            if(gridmx<0)
                gridmx+=mouseGridGap;
            if(gridmy<0)
                gridmy+=mouseGridGap;

            g.setColor(mouseMajorColor);
            g.setStroke(new BasicStroke(mouseGridWidth));
            for(int i = gridmx; i < mouseWidth; i+=mouseGridGap){
                g.drawLine(mouseX+i, mouseY+gw2, mouseX+i, mouseY+mouseHeight-mouseGridWidth);
            }
            for(int i = gridmy; i < mouseHeight; i+=mouseGridGap){
                g.drawLine(mouseX+gw2, mouseY+i, mouseX+mouseWidth-mouseGridWidth, mouseY+i);
            }
        }
        
        //Calculate Ball Offset
        int ballAddX = (int)(mouseVelX*ballmul) - mouseBallSize/2;
        int ballAddY = (int)(mouseVelY*ballmul) - mouseBallSize/2;
        int ballPosX = clamp(midx+ballAddX,mouseX,mouseX+mouseWidth-mouseBallSize);
        int ballPosY = clamp(midy+ballAddY,mouseY,mouseY+mouseHeight-mouseBallSize);
        
        //Draw Ball Thread Border
        g.setStroke(new BasicStroke(mouseBallThreadBorderSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(mouseMajorColor);
        g.drawLine(midx, midy, ballPosX+mouseBallSize/2, ballPosY+mouseBallSize/2);
        //Draw Ball Thread
        g.setStroke(new BasicStroke(mouseBallThreadSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(mouseMinorColor);
        g.drawLine(midx, midy, ballPosX+mouseBallSize/2, ballPosY+mouseBallSize/2);
        
        //Draw Ball Border then Ball
        g.fillOval(ballPosX, ballPosY, mouseBallSize, mouseBallSize);
        g.setColor(mouseMajorColor);
        g.fillOval(ballPosX+mouseBallBorderSize/2, ballPosY+mouseBallBorderSize/2, mouseBallSize-mouseBallBorderSize, mouseBallSize-mouseBallBorderSize);
        
        //Draw Buttons
        int lrbwidth = (mouseWidth-mouseMiddleButtonWidth-mouseButtonGap*2)/2;
        //Left Button
        g.setColor(leftClickPressed?mouseButtonOnColor:mouseButtonOffColor);
        g.fillRect(mouseX, mouseY, lrbwidth, mouseButtonHeight);
        //Right Button
        g.setColor(rightClickPressed?mouseButtonOnColor:mouseButtonOffColor);
        g.fillRect(mouseX+mouseWidth-lrbwidth, mouseY, lrbwidth, mouseButtonHeight);
        //Middle Button
        g.setColor(middleClickPressed?mouseButtonOnColor:mouseButtonOffColor);
        int middlebutposx = mouseX+lrbwidth+mouseButtonGap;
        g.fillRect(middlebutposx, mouseY, mouseMiddleButtonWidth, mouseButtonHeight);
        //Mouse Wheel Setup
        int[] tx = new int[3]; //Top, Bot right, Bot left
        int[] ty = new int[3];
        tx[0] = middlebutposx+mouseMiddleButtonWidth/2;
        tx[1] = middlebutposx+mouseMiddleButtonWidth-mouseWheelPadding;
        tx[2] = middlebutposx+mouseWheelPadding;
        //Mouse Wheel Up
        long wheeluptime = System.currentTimeMillis()-lastWheelUp;
        if(wheeluptime < mouseWheelFadeout){
            g.setColor(colMulAlpha(mouseWheelColor, 1.0-(double)wheeluptime/mouseWheelFadeout));
            ty[0] = mouseY+mouseWheelPadding;
            ty[1] = mouseY+mouseMiddleButtonWidth-mouseWheelPadding*2;
            ty[2] = ty[1];
            g.fillPolygon(tx, ty, 3);
        }
        //Mouse Wheel Down
        long wheeldowntime = System.currentTimeMillis()-lastWheelDown;
        if(wheeldowntime < mouseWheelFadeout){
            g.setColor(colMulAlpha(mouseWheelColor, 1.0-(double)wheeldowntime/mouseWheelFadeout));
            ty[0] = mouseY+mouseButtonHeight-mouseWheelPadding;
            ty[1] = mouseY+mouseButtonHeight-mouseMiddleButtonWidth+mouseWheelPadding*2;
            ty[2] = ty[1];
            g.fillPolygon(tx, ty, 3);
        }
        
        //Draw Border
        if(mouseDrawBorder){
            g.setColor(mouseMinorColor);
            g.setStroke(new BasicStroke(mouseBorderWidth));
            int mbw2 = mouseBorderWidth/2;
            g.drawRect(mouseX+mbw2, mouseY+mbw2, mouseWidth-mouseBorderWidth, mouseHeight-mouseBorderWidth);
        }
    }
    
    private int clamp(int a, int b, int c){
        if(a<b)
            return b;
        else if(a>c)
            return c;
        else return a;
    }
    
    private Color getKeyCol(KeyInfo ki){
        if(ki.pressed) return ki.onColor;
        
        return ki.offColor;
    }
    
    private Color getOppKeyCol(KeyInfo ki){
        if(ki.pressed) return ki.offColor;
        
        return ki.onColor;
    }
    
    private Color flipKeyCol(KeyInfo ki, Color c){
        //PROBABLY USELESS
        int rd = ki.onColor.getRed() - ki.offColor.getRed();
        int gd = ki.onColor.getGreen() - ki.offColor.getGreen();
        int bd = ki.onColor.getBlue() - ki.offColor.getBlue();
        int ad = ki.onColor.getAlpha() - ki.offColor.getAlpha();
        
        int dsum = rd+gd+bd+ad;
        
        int crd = ki.onColor.getRed() - c.getRed();
        int cgd = ki.onColor.getGreen() - c.getGreen();
        int cbd = ki.onColor.getBlue() - c.getBlue();
        int cad = ki.onColor.getAlpha() - c.getAlpha();
        
        int cdsum = crd+cgd+cbd+cad;
        
        //Check which color it is closer to
        if(cdsum<dsum/2){
            //Closer to onColor, return a color closer to offColor
            //return newColor(ki.offColor.getRed())
        }
        else{
            //Closer to offColor, return a color closer to onColor
        }
        
        return c;
    }
    
    private double getKeyAlphaMul(KeyInfo ki){
        if(!keyUseFadeout || ki.pressed)
            return 1.0;
        
        long timediff = System.currentTimeMillis()-ki.lastPressed;
        
        if(timediff > keyFadeoutHangTime+keyFadeoutTime)
            return 0.0;
        
        if(timediff < keyFadeoutHangTime)
            return 1.0;
        
        return 1.0 - (double)(timediff-keyFadeoutHangTime)/keyFadeoutTime;
    }
    
    private Color colMulAlpha(Color c, double alpha){
        int newAlpha = (int)(c.getAlpha()*alpha);
        
        return new Color(c.getRed(),c.getGreen(),c.getBlue(),newAlpha);
    }
    
    //Set up keyboard for quick drawing
    
    private void setupKeyboard(int x, int y){
        setupRow0(x+keyPadding, y+keyPadding);
        setupRow1(x+keyPadding, y+keyHeight+keyPadding*2);
        setupRow2(x+keyPadding, y+keyHeight*2+keyPadding*3);
        setupRow3(x+keyPadding, y+keyHeight*3+keyPadding*4);
        setupRow4(x+keyPadding, y+keyHeight*4+keyPadding*5);
        //Text Overrides for specific keys
        keyArr[192].text = "`";
        keyArr[189].text = "-";
        keyArr[187].text = "=";
        keyArr[219].text = "[";
        keyArr[221].text = "]";
        keyArr[186].text = ";";
        keyArr[222].text = "'";
        keyArr[188].text = ",";
        keyArr[190].text = ".";
        keyArr[191].text = "/";
        
    }
    
    int[] keyRow0 = new int[] {192, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 189, 187, 8};
    int[] keyRow1 = new int[] {9, 81, 87, 69, 82, 84, 89, 85, 73, 79, 80, 219, 221, 220};
    int[] keyRow2 = new int[] {20, 65, 83, 68, 70, 71, 72, 74, 75, 76, 186, 222, 13};
    int[] keyRow3 = new int[] {16, 90, 88, 67, 86, 66, 78, 77, 188, 190, 191, 16};
    int[] keyRow4 = new int[] {17, 91, 18, 32, 18, 92, 17};
    
    private void setupRow0(int x, int y){
        int i = 0;
        //All except last
        for(; i < keyRow0.length-1; i++){
            keyArr[keyRow0[i]] = new KeyInfo((""+(char)keyRow0[i]).toUpperCase(), x + (keyWidth+keyPadding)*i, y, keyWidth, keyHeight);
        }
        
        //Backspace; 2 keys worth
        keyArr[8] = new KeyInfo("BKSPC", x + (keyWidth+keyPadding)*i, y, keyWidth*2 + keyPadding, keyHeight);
    }
    
    private void setupRow1(int x, int y){
        //Tab; 1.75 keys worth
        int offset = (int)(keyWidth*1.75+keyPadding*0.75)+keyPadding;
        keyArr[9] = new KeyInfo("TAB", x,y,offset-keyPadding,keyHeight);
                
        //Rest
        int i = 1;
        for(; i < keyRow1.length-1; i++){
            keyArr[keyRow1[i]] = new KeyInfo((""+(char)keyRow1[i]), x + offset + (keyWidth+keyPadding)*(i-1), y, keyWidth, keyHeight);
        }
        
        //Backslash; 1.25 keys worth
        keyArr[220] = new KeyInfo("\\", x + offset + (keyWidth+keyPadding)*(i-1), y, (int)(Math.ceil(keyWidth*1.25+keyPadding*0.25)), keyHeight);
    }
    
    private void setupRow2(int x, int y){
        //Caps Lock; 2 keys worth
        int offset = keyWidth*2+keyPadding*2;
        keyArr[20] = new KeyInfo("CAPS", x,y,offset-keyPadding,keyHeight);
        
        //Rest
        int i = 1;
        for(; i < keyRow2.length-1; i++){
            keyArr[keyRow2[i]] = new KeyInfo((""+(char)keyRow2[i]).toUpperCase(), x + offset + (keyWidth+keyPadding)*(i-1), y, keyWidth, keyHeight);
        }
        
        //Enter; 2 keys worth
        keyArr[13] = new KeyInfo("ENTER", x + offset + (keyWidth+keyPadding)*(i-1), y, keyWidth*2+keyPadding, keyHeight);
    }
    
    private void setupRow3(int x, int y){
        //Shift; 2.5 keys worth
        int offset = (int)(keyWidth*2.5+keyPadding*1.5)+keyPadding;
        keyArr[16] = new KeyInfo("SHIFT", x,y,offset-keyPadding,keyHeight);
        
        //Rest
        int i = 1;
        for(; i < keyRow3.length-1; i++){
            keyArr[keyRow3[i]] = new KeyInfo((""+(char)keyRow3[i]).toUpperCase(), x + offset + (keyWidth+keyPadding)*(i-1), y, keyWidth, keyHeight);
        }

        //Shift; 2.5 keys worth
        //Duplicate keys not implemented yet
    }
    
    private void setupRow4(int x, int y){
        //Ctrl,Win,Alt; 1.5, 1, 1.5
        int offset = (int)(keyWidth*1.5+keyPadding*0.5)+keyPadding;
        keyArr[17] = new KeyInfo("CTRL", x, y, offset-keyPadding, keyHeight);
        
        keyArr[91] = new KeyInfo("WIN", x+offset,y,keyWidth,keyHeight);
        offset += keyWidth+keyPadding;
        
        keyArr[18] = new KeyInfo("ALT", x+offset,y,(int)(keyWidth*1.5+keyPadding*0.5),keyHeight);
        offset += (int)(keyWidth*1.5+keyPadding*0.5)+keyPadding;
        
        //Spacebar; 6 keys worth
        keyArr[32] = new KeyInfo("SPACE", x+offset,y,keyWidth*6,keyHeight);
        
        //Alt,Win,Ctrl
        
    }
}
