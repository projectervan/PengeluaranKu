# Product Requirements Document (PRD)

**Nama Proyek:** FinansialKu (Aplikasi Pencatat Keuangan Bulanan Berbasis Lokal)  
**Versi:** 1.0.0  
**Target Platform:** Android (Google Play Store)  
**Bahasa Pemrograman / Framework:** Kotlin (Native dengan Jetpack Compose)
**Arsitektur Sistem:** MVVM (Model-View-ViewModel) / Clean Architecture  

---

## 1. PENDAHULUAN & RINGKASAN PRODUK

### 1.1 Latar Belakang
Banyak pengguna menginginkan aplikasi pencatat keuangan yang cepat, privat, dan tidak memerlukan biaya langganan bulanan. Sebagian besar aplikasi di pasar menggunakan cloud database yang memperlambat performa saat koneksi buruk dan mengorbankan privasi data finansial pengguna.

### 1.2 Tujuan Produk
Membangun aplikasi Android untuk mencatat pemasukan dan pengeluaran bulanan secara *offline-first* (lokal penuh), dengan fitur otomatisasi pengeluaran rutin wajib (seperti tagihan), serta akses masuk menggunakan Google Sign-In via Firebase untuk verifikasi identitas unik pengguna tanpa menyimpan data finansial mereka di cloud.

---

## 2. BATASAN TEKNIS & ARSITEKTUR (TEKANKAN PADA AI AGENT)

**PANDUAN MUTLAK UNTUK AI AGENT:**
1. **Offline-First / Zero Cloud Database:** Dilarang keras menggunakan Firebase Firestore, Realtime Database, Supabase, AWS, atau REST API eksternal untuk menyimpan transaksi. Semua data keuangan *harus* disimpan di penyimpanan lokal perangkat menggunakan **Room Database (SQLite)** jika native, atau **sqflite/Hive** jika menggunakan Flutter.
2. **Fungsi Firebase Authentication:** Firebase hanya digunakan untuk memvalidasi Google Sign-In di sisi klien untuk mendapatkan User ID (`UID`) unik. `UID` ini digunakan secara lokal sebagai pengidentifikasi atau pengunci sesi aplikasi.
3. **Keamanan Data:** Karena data bersifat lokal, jika pengguna menghapus data aplikasi atau melakukan *uninstall*, data akan hilang (berikan informasi ini pada UI pengaturan jika diperlukan).

---

## 3. ARSITEKTUR DATA & SKEMA DATABASE LOKAL

AI Agent harus mengimplementasikan entitas database dengan struktur tabel relasional sebagai berikut:

### 3.1 Tabel: `users`
Digunakan untuk menyimpan profil lokal hasil dari Google Sign-In.
| Nama Kolom | Tipe Data | Atribut | Deskripsi |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR | PRIMARY KEY | Firebase UID pengguna |
| `name` | VARCHAR | NOT NULL | Nama lengkap dari akun Google |
| `email` | VARCHAR | NOT NULL | Email dari akun Google |
| `photo_url` | VARCHAR | NULLABLE | URL foto profil pengguna |

### 3.2 Tabel: `transactions`
Menyimpan semua data transaksi pemasukan dan pengeluaran.
| Nama Kolom | Tipe Data | Atribut | Deskripsi |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR / UUID | PRIMARY KEY | ID unik transaksi |
| `user_id` | VARCHAR | FOREIGN KEY | Relasi ke `users.id` |
| `type` | VARCHAR | NOT NULL | Nilai wajib: `"INCOME"` atau `"EXPENSE"` |
| `amount` | DOUBLE | NOT NULL | Nominal keuangan (Validasi: > 0) |
| `category` | VARCHAR | NOT NULL | Kategori (Contoh: Gaji, Makanan, Tagihan) |
| `date` | TIMESTAMP | NOT NULL | Tanggal dan waktu transaksi |
| `note` | TEXT | NULLABLE | Catatan tambahan |
| `is_recurring_generated` | BOOLEAN | DEFAULT `false` | `true` jika dibuat otomatis oleh sistem tagihan |

### 3.3 Tabel: `recurring_bills`
Menyimpan konfigurasi pengeluaran rutin bulanan yang diatur oleh pengguna.
| Nama Kolom | Tipe Data | Atribut | Deskripsi |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR / UUID | PRIMARY KEY | ID unik tagihan rutin |
| `user_id` | VARCHAR | FOREIGN KEY | Relasi ke `users.id` |
| `bill_name` | VARCHAR | NOT NULL | Nama tagihan (Contoh: "Token Listrik", "WiFi") |
| `amount` | DOUBLE | NOT NULL | Estimasi nominal tagihan |
| `category` | VARCHAR | NOT NULL | Kategori (Default biasanya "Tagihan / Rutinitas") |
| `due_day` | INTEGER | NOT NULL | Tanggal jatuh tempo bulanan (Rentang: 1 - 31) |
| `is_active` | BOOLEAN | DEFAULT `true` | Status aktif/nonaktif otomatisasi |
| `last_generated_month` | INTEGER | NULLABLE | Angka bulan terakhir kali tagihan ini di-generate (1-12) |
| `last_generated_year` | INTEGER | NULLABLE | Angka tahun terakhir kali tagihan ini di-generate (YYYY) |

---

## 4. FITUR UTAMA & KRITERIA PENERIMAAN (ACCEPTANCE CRITERIA)

### Fitur 4.1: Alur Autentikasi (Google Login)
* **Deskripsi:** Pengguna wajib login dengan Google sebelum masuk ke dashboard aplikasi.
* **Kriteria Penerimaan:**
  * Menampilkan halaman Welcome/Login dengan tombol "Masuk dengan Google" jika sesi lokal kosong.
  * Mengintegrasikan SDK Firebase Auth.
  * Setelah berhasil login, simpan informasi user ke tabel lokal `users`.
  * Sesi login harus presisten; jika aplikasi ditutup dan dibuka kembali, pengguna langsung masuk ke Dashboard tanpa login ulang.

### Fitur 4.2: Dashboard Ringkasan Keuangan
* **Deskripsi:** Menampilkan kondisi keuangan bulan berjalan secara visual dan ringkas.
* **Kriteria Penerimaan:**
  * Menampilkan **Total Saldo** Saat Ini (Rumus: `Total Pemasukan Bulan Ini - Total Pengeluaran Bulan Ini`).
  * Menampilkan card informasi terpisah untuk **Total Pemasukan Bulanan** dan **Total Pengeluaran Bulanan**.
  * Menampilkan diagram lingkaran (*Pie Chart*) kontribusi pengeluaran per kategori.
  * Menampilkan daftar 5 transaksi terbaru (*Recent Transactions*).
  * Menyediakan navigasi tombol pindah bulan (Panah Kiri: Bulan Sebelumnya, Panah Kanan: Bulan Berikutnya).

### Fitur 4.3: Pencatatan Transaksi Baru (CRUD)
* **Deskripsi:** Antarmuka untuk memasukkan, mengubah, dan menghapus data keuangan secara manual.
* **Kriteria Penerimaan:**
  * **Input Form:** Pilihan tipe (`INCOME`/`EXPENSE`), Input Angka (Nominal), Dropdown Kategori, Date Picker (Default hari ini), dan Text Field Catatan.
  * **Validasi Form:** Nominal tidak boleh kosong, minimal bernilai 1. Kategori tidak boleh kosong.
  * Menyediakan fitur hapus transaksi dengan konfirmasi dialog (*Alert Dialog*).
  * Perubahan data harus langsung meng-update komponen saldo di Dashboard secara reaktif (Gunakan LiveData / StateFlow / Stream).

### Fitur 4.4: Pengaturan Tagihan Rutin Pasti Bulanan (Recurring Expenses)
* **Deskripsi:** Fitur utama untuk mendaftarkan pos pengeluaran yang pasti terjadi setiap bulan (contoh: Internet, Kost, PDAM).
* **Kriteria Penerimaan:**
  * Halaman khusus bernama "Manajemen Tagihan Rutin".
  * Pengguna bisa menambahkan data tagihan dengan menentukan nama tagihan, perkiraan nominal, kategori, dan tanggal jatuh tempo bulanan.
  * Menyediakan *toggle* switch untuk mengaktifkan atau menonaktifkan tagihan rutin tertentu.

---

## 5. LOGIKA SISTEM (BUSINESS LOGIC FOR AI EXECUTION)

AI Agent harus mengimplementasikan fungsi otomatisasi pengecekan tanggal saat aplikasi pertama kali dibuka (*App Launch Lifecycle*).

### 5.1 Algoritma Otomatisasi Tagihan Rutin (`checkAndGenerateRecurringBills`)
Setiap kali pengguna berhasil melewati Splash Screen atau membuka aplikasi, jalankan logika berikut di latar belakang:
```pseudo
FUNGSI checkAndGenerateRecurringBills(current_user_id):
    current_date = AmbilTanggalSekarang()
    current_month = current_date.bulan (1-12)
    current_year = current_date.tahun (YYYY)
    current_day = current_date.hari (1-31)

    // Ambil semua tagihan rutin aktif milik user
    daftar_tagihan = QUERY "SELECT * FROM recurring_bills WHERE user_id = current_user_id AND is_active = true"

    UNTUK SETIAP tagihan DI DALAM daftar_tagihan:
        // Cek apakah tagihan sudah pernah di-generate untuk bulan dan tahun ini
        APABILA (tagihan.last_generated_month != current_month) ATAU (tagihan.last_generated_year != current_year):
            
            // Cek apakah hari ini sudah memasuki atau melewati tanggal jatuh tempo tagihan
            APABILA current_day >= tagihan.due_day:
                
                // 1. Masukkan ke tabel transaksi sebagai pengeluaran otomatis
                INSERT INTO transactions (
                    id = GenerateUUID(),
                    user_id = current_user_id,
                    type = "EXPENSE",
                    amount = tagihan.amount,
                    category = tagihan.category,
                    date = current_date,
                    note = "Dibuat otomatis oleh sistem: " + tagihan.bill_name,
                    is_recurring_generated = true
                )

                // 2. Perbarui status terakhir di-generate pada tabel tagihan rutin
                UPDATE recurring_bills 
                SET last_generated_month = current_month, last_generated_year = current_year
                WHERE id = tagihan.id
```
