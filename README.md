# Zero Touch Budget

Zero Touch Budget คือแอปพลิเคชันจัดการงบประมาณรายวันบน Android ที่ออกแบบมาเพื่อลดภาระการจดบันทึกรายรับรายจ่ายด้วยตนเองให้เหลือน้อยที่สุด ตัวแอปสอดแทรกความสามารถที่หลากหลายเพื่อช่วยให้การจัดการเงินเป็นไปอย่างอัตโนมัติ ทั้งการอ่านแจ้งเตือนจากธนาคาร, การสแกนและดึงข้อมูลจากใบเสร็จ (OCR + AI), ระบบสแกนรูปล่าสุดในพื้นหลัง และมี Widget บนหน้าจอโฮมเพื่อให้ผู้ใช้สามารถดูงบประมาณที่เหลืออยู่ของวันได้ง่ายๆ เพียงแค่เหลือบมอง

## 📖 อธิบายโปรเจ็คคร่าวๆ (Project Overview)

จุดประสงค์ของโปรเจ็คนี้คือสร้างแอปพลิเคชันจัดการงบประมาณที่เน้นการทำงานแบบ Local-first (เก็บข้อมูลในเครื่อง) และมี Workflow การใช้งานที่สะดวกที่สุด:
- **บันทึกอัตโนมัติเป็นหลัก:** ตรวจจับรายจ่ายอัตโนมัติจากแจ้งเตือนธนาคารหรือจากการดึงข้อมูลใบเสร็จ
- **บันทึกด้วยมือเป็นทางเลือก:** ผู้ใช้ยังคงสามารถเพิ่ม แก้ไข หรือลบรายการเองได้เสมอ
- **ประมวลผลบนเครื่อง:** ข้อมูลสรุปยอดใช้จ่ายและงบประมาณถูกบันทึกและคำนวณไว้บนอุปกรณ์ (Room & DataStore)
- **เข้าถึงข้อมูลไวสุด:** แสดงตัวเลขสำคัญที่สุด (งบเหลือเท่าไหร่) ไว้บน Widget บนตารางหน้าจอโฮม

*(คุณสามารถดูแผนผังการทำงานทั้งหมด (Wireframe) เพิ่มเติมได้ที่: `docs/WIREFRAME.md`)*

## 🛠 Tech Stack ที่ใช้

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3 Design
- **Dependency Injection:** Hilt
- **Local Database:** Room Database
- **Background Tasks:** WorkManager
- **Preferences / State:** Preferences DataStore
- **App Widget:** Glance App Widget
- **Text Recognition:** ML Kit Text Recognition
- **AI Integration:** Gemini API (ช่วยดึงข้อมูลและแยกแยะประเภทรายจ่ายจากข้อความ/ใบเสร็จ)
- **Architecture & Libraries:** MVVM Architecture, AndroidX (Lifecycle, Activity Compose, ExifInterface, DocumentFile)
- **Testing:** JUnit, MockK, และ Turbine

## ✨ Feature หลัก

- **Daily Budget Dashboard:** หน้าแดชบอร์ดหลักสำหรับดูสรุปงบประมาณที่ตั้งไว้, ยอดใช้จ่ายที่เกิดขึ้นแล้ว, และงบประมาณคงเหลือรายวัน
- **Manual Data Entry:** สามารถเพิ่ม แก้ไข ลบ รายการใช้จ่ายได้ด้วยตนเอง
- **Receipt Scanning (OCR):** อัปโหลดหรือถ่ายรูปภาพใบเสร็จเพื่อแยกตัวอักษรออกมา
- **AI Expense Extraction:** ใช้ AI (Gemini) ช่วยดึงยอดเงินและชื่อรายการออกจากภาพที่สแกน
- **Bank Notification Auto-tracking:** ตรวจจับและบันทึกรายจ่ายอัตโนมัติจากการแจ้งเตือนของแอปแอปพลิเคชันธนาคารต่างๆ
- **Auto-scan Background Service:** ระบบทำงานเบื้องหลังเพื่อตามหาภาพ Screenshot หรือภาพใหม่ๆ ในอัลบั้มที่กำหนดเพื่อนับเป็นรายจ่ายอัตโนมัติ
- **Home Screen Widget:** Widget ขนาดกะทัดรัดที่สะท้อนข้อมูลยอดเงินรายวัน ทำให้ไม่ต้องเปิดแอปก็รู้ได้ว่าเหลือเงินให้ใช้ได้อีกเท่าไหร่
- **Settings Screen:** กำหนดงบประมาณเป้าหมายรายวัน, เปิด/ปิดระบบ Auto-scan, และจัดการสิทธิ์เข้าถึงการแจ้งเตือน
- **Local Storage Privacy:** ข้อมูลธุรกรรมถูกนำไปจัดเก็บไว้อย่างปลอดภัยและเป็นส่วนตัวในฐานข้อมูลภายในเครื่อง

---

### การติดตั้งเบื้องต้น (Setup)

1. เปิดโปรเจ็คผ่าน **Android Studio**
2. เลือกรันบนสภาพแวดล้อม **JDK 17**
3. เตรียม Gemini API Key ของคุณ และเพิ่มลงไปที่ไฟล์ `gradle.properties`:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
4. Sync Gradle แล้วกดรันแอปลงใน Emulator หรือ Device ได้เลย
5. Wireframe
   <img width="1847" height="1001" alt="image" src="https://github.com/user-attachments/assets/af6eb943-c1bf-4094-9d02-7cacaaff07f7" />

