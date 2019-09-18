# AgoraIO-OnlineKaraoke

## ビルド方法
strings.xmlにAPP IDを入力します。  
https://github.com/fu-jimoto/AgoraIO-OnlineKaraoke/blob/master/app/src/main/res/values/strings.xml#L39

## 操作方法
Host x1, Audience x2の想定で実装しています。
### Host側
1.Hostにチェックをつけて「LOGIN」をクリックします。（RTMへのログイン）  
2.音質の設定をします。  
![設定値はこちら](https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_rtc_engine.html#a34175b5e04c88d9dc6608b1f38c0275d)  
3.「JOIN」をクリックします。（音声ネットワークへのログイン）  
4.audienceのmute/unmuteをクリックして指定したユーザーの音声配信/停止をコントロールします。  
(MUTEが配信停止、UNMUTEが配信）  
![host](https://user-images.githubusercontent.com/34727605/65112415-9082b200-da1a-11e9-8aa0-ff0ad3d64d2b.jpg)


### Audience側
1.Hostのチェックを外して「LOGIN」をクリックします。（RTMへのログイン）  
2.音質の設定をします。  
![設定値はこちら](https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_rtc_engine.html#a34175b5e04c88d9dc6608b1f38c0275d)  
3.「JOIN」をクリックします。（音声ネットワークへのログイン）  
4.SENDMESSAGE(MUTE/UNMUTE)をクリックしてHostに音声配信/停止をリクエストします。  
(MUTEが配信停止、UNMUTEが配信）

![audience](https://user-images.githubusercontent.com/34727605/65112414-9082b200-da1a-11e9-87b3-d6487be9e6dc.jpg)
