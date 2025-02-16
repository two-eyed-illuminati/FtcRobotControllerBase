package org.firstinspires.ftc.teamcode.utils;

public class Button{
    private boolean prevState = false;
    private final ButtonState buttonState;

    public Button(ButtonState buttonState){
        this.buttonState = buttonState;
    }

    public boolean isPressed(){
        boolean currentState = buttonState.get();
        return currentState && !prevState;
    }

    public boolean isReleased(){
        boolean currentState = buttonState.get();
        return !currentState && prevState;
    }

    public boolean isActuated(){
        return buttonState.get();
    }

    public void update(){
        prevState = buttonState.get();
    }
}
