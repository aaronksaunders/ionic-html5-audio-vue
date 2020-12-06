import Foundation
import Capacitor
import AVFoundation

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(AudioRecorder)
public class AudioRecorder: CAPPlugin, AVAudioRecorderDelegate {
    

    
    var recordingSession: AVAudioSession!
    var recorder: AVAudioRecorder!
    var fullPath: String = ""
    var duration: Double? = nil
    
    
    @objc func authorize(_ call: CAPPluginCall) {
        recordingSession = AVAudioSession.sharedInstance()
        if (recordingSession.responds(to: #selector(AVAudioSession.requestRecordPermission(_:)))) {
            AVAudioSession.sharedInstance().requestRecordPermission({(granted: Bool)-> Void in
                if granted {
                    call.resolve()
                } else {
                    call.reject("Audio Access Not Granted")
                }
            })
        }
    }
    
    func getUrl() -> URL {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        let documentsDirectory = paths[0]
        return documentsDirectory
    }
    
    func finishRecording(success: Bool) -> String {
        self.duration = recorder!.currentTime
        recorder.stop()

        if success {
            return "\(self.duration ?? 0.0)"
        } else {
            return "There was a problem recording your whistle; please try again."
        }
    }
    
    public func audioRecorderDidFinishRecording(_ recorder: AVAudioRecorder,
                                                  successfully flag: Bool) {
        self.recorder = nil
        let response : [String:Any] = [
            "file" : self.fullPath,
            "duration" : "\(self.duration ?? 0.0)"
        ]
        self.notifyListeners("recordingFinished", data: response)
        print(response)
        return
    }
    
    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.success([
            "value": value
        ])
    }
    
    
    
    @objc func start(_ call: CAPPluginCall) {
        
        do {
            try recordingSession.setCategory(.playAndRecord, mode: .default)
            try recordingSession.setActive(true)
        } catch {
            call.reject("Cannot start recording")
            return
        }
        
        guard let fileName = call.getString("fileName") else {
          call.reject("Must provide an fileName")
          return
        }
        
        self.duration = call.getDouble("duration") ?? nil
        
        print("Duration: \(String(describing: self.duration)), File \(fileName)")
        
        let settings = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 44100,
            AVNumberOfChannelsKey: 2,
            AVEncoderAudioQualityKey: AVAudioQuality.max.rawValue
        ]
        
        do {
            // 5
            self.fullPath = getUrl().appendingPathComponent(fileName).absoluteString;
            self.recorder = try AVAudioRecorder(url: getUrl().appendingPathComponent(fileName), settings: settings)
            self.recorder.delegate = self
            if (self.duration == nil) {
                self.recorder.record()
            } else {
                self.recorder.record(forDuration: self.duration ?? 6000.0)
            }
        } catch {
            let result = finishRecording(success: false);
            call.reject(result)
            return;
        }
        
        call.resolve([
            "resp": "recording started"
        ])
        return
    }
    
    @objc func stop(_ call: CAPPluginCall) {
        finishRecording(success: true)
        self.recorder = nil
        call.resolve([
            "duration" : self.duration!,
            "file" : self.fullPath

        ])
        return
    }
}
