package inputdisplay;

import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.mouse.GlobalMouseHook;
import lc.kra.system.mouse.event.GlobalMouseAdapter;
import lc.kra.system.mouse.event.GlobalMouseEvent;

public class InputDisplay {

    public static void main(String[] args) {
        InputDisplayFrame idf = new InputDisplayFrame();
        
        GlobalKeyboardHook gkh = new GlobalKeyboardHook(true);
        gkh.addKeyListener(new GlobalKeyAdapter(){
            public void keyPressed(GlobalKeyEvent e){
                idf.keyStateChange(e.getVirtualKeyCode(), true);
            }

            public void keyReleased(GlobalKeyEvent e){
                idf.keyStateChange(e.getVirtualKeyCode(), false);
            }
        });

        GlobalMouseHook gmh = new GlobalMouseHook(false);

        gmh.addMouseListener(new GlobalMouseAdapter(){
            public void mousePressed(GlobalMouseEvent e){
                //1 = Left; 2 = Right; 16 = Middle
                idf.mouseStateChange(e.getButton(),true);
                //System.out.println("Mouse Pressed: "+e.getButton());
            }

            public void mouseReleased(GlobalMouseEvent e){
                idf.mouseStateChange(e.getButton(),false);
                //System.out.println("Mouse Released: "+e.getButton());
            }

            public void mouseMoved(GlobalMouseEvent e){
                idf.mousePosChange(e.getX(), e.getY());
                //System.out.println("Mouse Moved: "+e.getX()+" "+e.getY());
            }

            public void mouseWheel(GlobalMouseEvent e){
                //Positive = wheel up; Negative = wheel down
                idf.mouseWheelUse(e.getDelta());
            }
        });
        
        //while(true){
        //    try{Thread.sleep(1000);}catch(Exception e){}
        //}
    }

}
