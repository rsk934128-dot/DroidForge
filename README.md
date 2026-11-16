<div align="center">

<img src="app/src/main/res/drawable/ic_forge_foreground_1794814650092.jpg" alt="DroidForge App Icon" width="110" style="border-radius: 24px;" />

# DroidForge v1.0

**মোবাইল-ফার্স্ট Cloud CI/CD IDE, AI এরর ডিবাগার, এবং নিরাপদ Pull Request জেনারেটর**  
*Mobile-First Cloud-Native Android CI/CD Studio with GitHub OAuth2, In-App Build Log Viewer, Firebase AI Logic Error Debugger, Line-Jumping Code Editor, Unified Diff Engine, Safe PR Workflow, and Room Offline Auto-Save Drafts.*

[![Version](https://img.shields.io/badge/Version-v1.0.0-00E59B?style=for-the-badge)](CHANGELOG.md)
[![Android](https://img.shields.io/badge/Platform-Android%20(Kotlin%20%2B%20Compose)-00E59B?style=for-the-badge&logo=android&logoColor=002114)](https://developer.android.com/compose)
[![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-38BDF8?style=for-the-badge&logo=githubactions&logoColor=002638)](https://docs.github.com/en/actions)
[![Firebase AI](https://img.shields.io/badge/AI%20Engine-Firebase%20AI%20Logic-FBBF24?style=for-the-badge&logo=firebase&logoColor=2E1E00)](https://firebase.google.com/docs/ai-logic)
[![Vercel](https://img.shields.io/badge/Web%20Portal-Vercel%20Ready-FBBF24?style=for-the-badge&logo=vercel&logoColor=2E1E00)](https://vercel.com)

<br />

<img src="app/src/main/res/drawable/img_ci_hero_1794814650260.jpg" alt="DroidForge Cloud CI/CD Hero Banner" width="100%" />

</div>

---

## অ্যাপের সংক্ষিপ্ত পরিচিতি (Overview)

**DroidForge** হলো একটি আধুনিক Material 3 (**Obsidian CI Terminal**) ডিজাইনের অ্যান্ড্রয়েড অ্যাপ, যা আপনার মোবাইল ফোন থেকেই সম্পূর্ণ সফটওয়্যার ডেভেলপমেন্ট ও রিলিজ সাইকেল পরিচালনা করতে সক্ষম:

1. **GitHub OAuth2 (PKCE) & PAT Authentication:** প্রাইভেট ও পাবলিক রিপোজিটরি অ্যাক্সেস।
2. **ক্লাউড APK বিল্ড ও লাইভ লগ ভিউয়ার:** বিল্ড স্ট্যাটাস পোলিং, ANSI স্ট্রিপিং ও এরর হাইলাইটিং।
3. **Firebase AI Logic (Gemini + App Check):** স্যানিটাইজড লগ কনটেক্সট থেকে রুট কজ বিশ্লেষণ ও ১-ট্যাপ **AI Auto-Patch**।
4. **Line-Jumping Code Editor + Safe PR:** এরর লাইন থেকে সরাসরি কোড এডিট, লাইন-বাই-লাইন Unified Diff এবং নতুন ব্রাঞ্চে নিরাপদ Pull Request তৈরি।
5. **Room DB Offline Draft Auto-Save & Activity Dashboard:** অফলাইনে অসম্পূর্ণ এডিট অটো-সেভ, হোম ড্যাশবোর্ডে সর্বশেষ বিল্ড, চলমান PR ও ড্রাফট ট্র্যাকিং।

অ্যাপটির ভেতরেই **English** এবং **বাংলা** দুই ভাষায় ব্যবহার করার সুবিধা রয়েছে।

<div align="center">
  <img src="docs/images/ui-showcase.jpg" alt="DroidForge Interface Showcase" width="100%" />
</div>

---

## মূল ফিচারসমূহ (Key Features — v1.0.0)

### ১. অ্যাক্টিভিটি ড্যাশবোর্ড, রিপো এক্সপ্লোরার ও অফলাইন ড্রাফট (`Repos`)
- **Activity Overview:** হোম স্ক্রিন থেকেই সক্রিয় রিপোজিটরির **সর্বশেষ বিল্ড স্ট্যাটাস**, **চলমান Pull Request সংখ্যা**, এবং **সংরক্ষিত অফলাইন ড্রাফট সংখ্যা** এক নজরে দেখা যায়।
- **লাইভ এক্সপ্লোর ও My Repos:** যেকোনো GitHub ইউজারনেম (`rsk934128-dot`), `owner/repo`, বা OAuth2 সাইন-ইনের মাধ্যমে প্রাইভেট ও পাবলিক রিপো লোড করা যায়।
- **Offline Drafts ট্যাব:** ইন্টারনেট না থাকলেও এডিটরে করা অসম্পূর্ণ কোড পরিবর্তনগুলো Room Database-এ সংরক্ষিত থাকে এবং এক ট্যাপে **Resume** করা যায়।

### ২. ক্লাউড CI/CD বিল্ড, ওপেন PR ও লগ ইন্সপেক্টর (`Builds` & `Run Inspector`)
- **Trigger Build (`workflow_dispatch`):** মোবাইল থেকেই ব্রাঞ্চ ও ওয়ার্কফ্লো সিলেক্ট করে নতুন Gradle APK বিল্ড চালু করা যায়।
- **Open Pull Requests:** সক্রিয় রিপোর চলমান PR-গুলো (নম্বর, শিরোনাম ও ব্রাঞ্চ) বিল্ড স্ক্রিন থেকেই দেখা ও GitHub-এ খোলা যায়।
- **In-App Build Log Viewer:** ANSI কালার কোড ও টাইমস্ট্যাম্প পরিষ্কার করে `Errors Only` এবং `Full Log` ফিল্টারসহ মনোস্পেস লগ ভিউ।
- **APK ও আর্টিফ্যাক্ট ডাউনলোড:** GitHub Releases থেকে `.apk` বা Actions Artifact `.zip` সরাসরি ডিভাইসে সেভ করা যায়।

### ৩. Firebase AI Logic ডিবাগার ও লাইন-জাম্পিং কোড এডিটর (`AI-Explain` & `Editor`)
- **নিরাপদ AI বিশ্লেষণ (`JobLogParser`):** Gemini-তে পাঠানোর আগে GitHub টোকেন (`ghp_...`, `Bearer ...`) এবং লোকাল হোম পাথ স্বয়ংক্রিয়ভাবে `[REDACTED_TOKEN]` ও `<USER_HOME>` দিয়ে মাস্ক করা হয়।
- **Open File ও Line-Jumping:** বিল্ড এররের ফাইল পাথ ও লাইন নম্বর স্বয়ংক্রিয়ভাবে চিহ্নিত করে সরাসরি কোড এডিটরে সেই লাইনে জাম্প ও হাইলাইট করা যায়।
- **AI Auto-Patch & Unified Diff (`DiffEngine`):** LCS ভিত্তিক লাইন-বাই-লাইন `+`/`-` Diff রিভিউ করে কোড যাচাই করা যায়।
- **Safe PR জেনারেটর:** সরাসরি `main`-এ পুশ না করে স্বয়ংক্রিয়ভাবে `droidforge/fix-...` ব্রাঞ্চ তৈরি করে কমিট ও Pull Request খোলা হয়।
- **Auto-Save Drafts:** টাইপ করা বা AI প্যাচ দেওয়ার ~১.২ সেকেন্ডের মধ্যে Room DB-তে ড্রাফট সেভ হয় এবং PR সফল হলে স্বয়ংক্রিয়ভাবে পরিষ্কার হয়।

### ৪. GitHub Actions YAML ও Vercel ওয়েব পোর্টাল স্টুডিও (`YAML / Studio`)
- **Actions YAML জেনারেটর:** `assembleDebug`, `assembleRelease`, বা `bundleRelease` টাস্ক, JDK (`17` / `21`), এবং অটো-রিলিজ অপশন দিয়ে `.github/workflows/android-apk.yml` পুশ করা যায়।
- **Vercel ওয়েবসাইট পোর্টাল:** APK ডাউনলোড পোর্টাল (`index.html` + `vercel.json`) তৈরি করে এক ক্লিকে GitHub-এ পুশ করা যায়।

---

## ক্লাউড বিল্ড আর্কিটেকচার (Architecture)

<div align="center">
  <img src="docs/images/cicd-flow.jpg" alt="Cloud CI/CD Architecture Flow" width="100%" />
</div>

1. **Networking (`GitHubApiService` & `GitHubOAuthManager`):** Retrofit + Moshi + OkHttp দিয়ে GitHub REST API v3 এবং OAuth2 PKCE ফ্লো পরিচালনা করে।
2. **AI Engine (`AIErrorService` + `AppCheckInitializer`):** Firebase AI Logic (`Firebase.ai`) এবং App Check (Debug / Play Integrity / reCAPTCHA) ব্যবহার করে নিরাপদ Gemini ইন্টিগ্রেশন।
3. **Domain Engines (`JobLogParser` & `DiffEngine`):** ANSI ক্লিনিং, টোকেন স্যানিটাইজেশন, ফাইল-লাইন এক্সট্র্যাকশন এবং LCS Diff কম্পিউটেশন।
4. **Local Persistence (`Room` v2 & `DataStore`):** বুকমার্ক রিপো, ক্যাশড রান, অফলাইন এডিটর ড্রাফট (`EditorDraftEntity`), থিম ও ভাষা সংরক্ষণ।

---

## টেক স্ট্যাক (Tech Stack)

| ক্যাটাগরি | ব্যবহৃত প্রযুক্তি |
| :--- | :--- |
| **Language & UI** | Kotlin, Jetpack Compose, Material Design 3 |
| **Architecture** | MVVM (`ViewModel` + `StateFlow`), Repository Pattern |
| **Networking & Auth** | Retrofit 2, OkHttp 4, Moshi (Codegen via KSP), GitHub OAuth2 PKCE |
| **AI & Security** | Firebase AI Logic (Gemini), Firebase App Check, Token Redaction |
| **Local Storage** | Room Database (v2), Jetpack DataStore Preferences |
| **Media & Fonts** | Coil Compose, Bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`) |
| **Testing** | JUnit 4, Robolectric, Coroutines Test |

---

## সেটআপ ও ব্যবহার নির্দেশিকা (Getting Started)

1. প্রজেক্টটি Gradle দিয়ে বিল্ড করুন:
   ```bash
   gradle assembleDebug
   ```
2. ইউনিট ও Robolectric টেস্ট চালান:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. অ্যাপটি ওপেন করে **Settings** ট্যাব থেকে **Sign in with GitHub (OAuth2)** অথবা আপনার **Personal Access Token (`repo` ও `workflow` scope)** যুক্ত করুন (পাবলিক রিপো ও লগ দেখার জন্য টোকেন ছাড়াও চলবে)।
4. **Repos** থেকে রিপোজিটরি বেছে **Builds** ট্যাবে বিল্ড ট্র্যাক করুন এবং কোনো এরর হলে **View Logs -> AI-Explain -> Open File** দিয়ে মোবাইল থেকেই Fix PR পাঠান!

---

## বিস্তারিত ভার্সন ইতিহাস

সকল রিলিজ নোটের জন্য [CHANGELOG.md](CHANGELOG.md) দেখুন।
