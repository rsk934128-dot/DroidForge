# Changelog — DroidForge

সকল উল্লেখযোগ্য পরিবর্তন ও রিলিজের বিবরণ এখানে সংরক্ষিত রয়েছে।  
*All notable changes and releases for DroidForge are documented in this file.*

---

## [v1.0.0] — 2026-11-16 (Stable Release)

### নতুন ফিচারসমূহ (Added)
- **GitHub OAuth2 Browser Login (PKCE):**
  - `droidforge://oauth/callback` ডিপ-লিংক ও CSRF state ভেরিফিকেশনসহ ১-ট্যাপ ব্রাউজার লগইন।
- **In-App Build Log Viewer & Error Highlighter:**
  - `JobLogParser`-এর মাধ্যমে ANSI কোড ও টাইমস্ট্যাম্প পরিষ্কার করে `Errors Only` ও `Full Log` ফিল্টার ভিউ।
  - দীর্ঘ লগ নিরাপদ ৭৫০ KB স্ট্রিমিংয়ে ডাউনলোড ও ক্লিপবোর্ড কপি সুবিধা।
- **Firebase AI Logic Error Debugger (`AI-Explain` & `AI Auto-Patch`):**
  - `Firebase.ai` (`gemini-3.8-flash`) ও App Check সুরক্ষাসহ স্ট্রাকচার্ড JSON এরর বিশ্লেষণ (Root Cause, Fix Steps, Suggested Fix)।
  - AI-তে পাঠানোর আগে GitHub PAT, Bearer টোকেন ও লোকাল হোম পাথ স্বয়ংক্রিয়ভাবে `[REDACTED_TOKEN]` ও `<USER_HOME>` দিয়ে স্যানিটাইজ করা হয়।
- **Line-Jumping Code Editor, Unified Diff & Safe PR Generator:**
  - Kotlin ও Gradle বিল্ড এরর থেকে ফাইল পাথ এবং লাইন নম্বর এক্সট্র্যাক্ট করে সরাসরি কোড এডিটরে জাম্প ও হাইলাইট।
  - `DiffEngine`-এর মাধ্যমে লাইন-বাই-লাইন `+`/`-` Unified Diff প্রিভিউ।
  - নতুন ফিক্স ব্রাঞ্চ (`droidforge/fix-...`) তৈরি করে নিরাপদ Pull Request সাবমিশন।
- **Room DB Offline Draft Auto-Save:**
  - `AppDatabase` v2-তে `EditorDraftEntity` ও `EditorDraftDao` যুক্ত করা হয়েছে।
  - কোড বা PR মেটাডেটা এডিট করার ~১.২ সেকেন্ড পর অটো-সেভ, ফাইল পুনরায় ওপেন বা অফলাইনে রিজিউম করার সুবিধা, এবং PR সফল হলে বা Reset করলে অটো-ক্লিনআপ।
- **Activity Overview Dashboard & Open PR Tracking:**
  - হোম (`Repos`) স্ক্রিনে সর্বশেষ বিল্ড স্ট্যাটাস, চলমান PR সংখ্যা এবং সংরক্ষিত অফলাইন ড্রাফট কার্ড ও ডেডিকেটেড **Drafts** ট্যাব।
  - `Builds` স্ক্রিনে রিপোজিটরির চলমান **Open Pull Requests** তালিকা ও সরাসরি GitHub-এ খোলার বাটন।

### পরীক্ষা ও মানোন্নয়ন (Testing & Quality)
- **Unit Tests:** `JobLogParser` (ANSI, redaction, file/line extraction), `DiffEngine`, এবং `draftKeyFor` নরমালাইজেশন টেস্ট।
- **Robolectric Tests:** OAuth2 PKCE URI / CSRF যাচাই এবং Room ইন-মেমোরি `EditorDraftDao` CRUD + `Flow` টেস্ট।
- **বilingual UI:** সম্পূর্ণ অ্যাপ জুড়ে English ও বাংলা (`AppStrings.kt`) দ্বিভাষিক সাপোর্ট।
