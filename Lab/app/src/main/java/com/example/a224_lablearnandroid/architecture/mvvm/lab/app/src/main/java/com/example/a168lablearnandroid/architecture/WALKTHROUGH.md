# คู่มือการเรียนรู้ Android Architecture Patterns

ตัวอย่างแอปพลิเคชัน "Counter" อย่างง่าย เพื่อเปรียบเทียบโครงสร้างแบบต่างๆ ให้นักศึกษาเข้าใจได้ชัดเจน โดยโค้ดทั้งหมดจะอยู่ที่ `com.example.lablearnandroid.ui.architecture`

## โครงสร้างไฟล์ (File Structure)

```
com.example.lablearnandroid.ui.architecture
├── mvc/        # Model-View-Controller
├── mvp/        # Model-View-Presenter
├── mvvm/       # Model-View-ViewModel
└── mvi/        # Model-View-Intent
```

## 1. MVC (Model-View-Controller)
**แนวคิด:** Activity ทำหน้าที่เป็นทั้ง View และ Controller

- **Activity (`MvcCounterActivity`):**
    - รับ User Input (การกดปุ่ม)
    - เรียก Model (`model.incrementCounter()`)
    - อัปเดต UI (`tvCount.text = ...`)
- **Model (`MvcCounterModel`):**
    - เก็บค่า `count` และ Business Logic การบวกเลข

**ข้อสังเกต:** ใน Android, Controller มักจะถูกรวมผูกติดอยู่กับ View (Activity/Fragment) ทำให้คลาสมีขนาดใหญ่และทดสอบยาก

## 2. MVP (Model-View-Presenter)
**แนวคิด:** แยก Logic ออกจาก View โดยใช้ Presenter คั่นกลาง เพื่อให้ View มีหน้าที่แค่แสดงผล (Passive View)

- **View (`MvpCounterActivity` implements `MvpCounterView`):**
    - ทำหน้าที่แค่รับ Click แล้วแจ้ง Presenter (`presenter.onIncrementClicked()`)
    - รอรับคำสั่งจาก Presenter ย้อนกลับมาเพื่ออัปเดตหน้าจอ (`showCount(...)`) ผ่าน Interface `MvpCounterView`
- **Presenter (`MvpCounterPresenter`):**
    - ตัวกลางในการตัดสินใจ รับคำสั่งจาก View -> สั่ง Model -> คำนวณเสร็จแล้วสั่ง View ให้แสดงผล
- **Model (`MvpCounterModel`):**
    - เก็บข้อมูลและ Logic เหมือนเดิม

## 3. MVVM (Model-View-ViewModel)
**แนวคิด:** View "สังเกตการณ์" (Observe) ข้อมูลจาก ViewModel ผ่าน LiveData หรือ StateFlow

- **View (`MvvmCounterActivity` - Jetpack Compose):**
    - ใช้ `setContent` และ Composable Function `MvvmCounterScreen`
    - สังเกตการณ์ (Collect) `StateFlow` จาก ViewModel
- **ViewModel (`MvvmCounterViewModel`):**
    - ถือครองข้อมูลในรูปแบบ `StateFlow` (ทันสมัยกว่า LiveData)
    - ไม่มี Reference ถึง View โดยตรง
- **Model (`MvvmCounterModel`):**
    - Repository หรือแหล่งข้อมูล

## 4. MVI (Model-View-Intent)
**แนวคิด:** การไหลของข้อมูลทางเดียว (Unidirectional Data Flow) และสถานะเดียว (Single Source of Truth)

- **Intent (`CounterIntent`):**
    - นิยามการกระทำของผู้ใช้ทั้งหมดที่เป็นไปได้ (เช่น `IncrementCounter`)
- **State (`CounterState`):**
    - Data Class ที่เก็บสถานะทั้งหมดของหน้าจอ ณ ขณะนั้น
- **ViewModel (`CounterViewModel`):**
    - รับ Intent -> ประมวลผล Business Logic -> สร้าง State ใหม่ -> ปล่อย State ออกไป (`_state.value = ...`)
- **View (`CounterScreen` - Jetpack Compose):**
    - ส่ง Intent ไปให้ ViewModel
    - Observe State กลับมาเพื่อ Recompose UI

## วิธีการทดสอบ (How to Run)
เนื่องจากแต่ละ Architecture เป็น Activity แยกกัน หากต้องการรันตัวอย่างไหน ให้แก้ไข `AndroidManifest.xml` โดยย้าย `<intent-filter>` ที่มี `MAIN` และ `LAUNCHER` ไปไว้ที่ Activity ที่ต้องการทดสอบ

```xml
<activity android:name=".ui.architecture.mvi.MviCounterActivity" ... >
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```