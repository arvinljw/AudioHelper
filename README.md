# AudioHelper

这是一个音频的录制和播放的demo。

* 录制采用了AudioRecord+lame编码为mp3的方式实现
* 播放采用了MediaPlayer类实现

## 录制的使用

音频的录制变得很简单，使用AudioRecorder即可完成。

**使用方法：**初始化之后通过startRecord和stopRecord来录制一段音频，录制完成通过getRecordFile获得，期间可以设置VolumeCallback回调音量的大小，参考demo。

## 播放的使用

音频的播放，使用AudioPlayer即可完成。

使用方法：初始化完成之后，调用其play(url)方法即可播放，暂停可调用pause()方法，暂停之后再播放调用play()方法即可，结束播放可直接使用stop()方法，当然再次直接播放可以直接调用play(url)方法即可，但是如果是要播放其他url的音频，这时候需要release之后再重新初始化，再重新调用play(url)方法即可。

## License
```
Copyright 2021 arvinljw

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```