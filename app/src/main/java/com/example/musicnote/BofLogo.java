package com.example.musicnote;

import android.media.MediaPlayer;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class BofLogo extends Node {
    BofLogo(AnchorNode parent, ModelRenderable bofLogoModel,
            ArSceneView arSceneView){
        this.setRenderable(bofLogoModel);
        this.setLocalScale(new Vector3(.33f, .33f, .33f));
        this.setParent(parent);
        //this.setLocalPosition(this.getUp().scaled(+0.5f));

        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Vector3 up = this.getUp();

        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);
    }
}
