[![](https://jitpack.io/v/netherpyro/GlComposableView.svg)](https://jitpack.io/#netherpyro/GlComposableView)

# GLComposableView

**GLComposableView** is a view extended the **GLSurfaceView** with easy to use API to add, remove, order media layers on application level.

A layer holds surface or bitmap. The surface layer is responsible for displaying video. The bitmap layer displays static image.

Any layer can be easily transformed. Actions on a layer such as *scale*, *rotate*, *pan* are available in the `Transformable` interface returned to application after the layer added. In addition to these actions, you can change the opacity and set the presence of a border of the layer.

Transform actions can be optionally tracked by gestures performed on the view.

The view itself can be resized with animation smoothly. In opposite, the GLSurfaceView can't do that without noticeable lags –  that's the reason prompted me to make this library.

Please take a look at the sample app within this repo to try out the API.

## Add to your project 

Add it in your root **build.gradle** at the end of repositories:
```gradle
allprojects {
  repositories {
    
    maven { url 'https://jitpack.io' }
  }
}
```
Add the dependency in application **build.gradle**:
```
dependencies {
  implementation 'com.github.netherpyro:GlComposableView:1.5.0'
}
```

## Developing

In the `baker` branch that is under developing the video file renderer represented. 
You can "bake" your project with the selected media files into .mp4 file with handy API that extends the **GLCV** API. 

Project baker takes a template from a composed project with user's photo/video files. 
The user's media can be transformed in the same way as layers, thus **GLComposableView** is being used.

The result video file with the baked project within can be configured by render params such as FPS and resolution before start rendering process. 
Rendering process is performed by hardware Android's **MediaCodec** and **MediaMuxer**.

Please take a look at the sample app in the `baker` branch.

Feel free to send a PR to help me out with Baker or fixes 🙂
