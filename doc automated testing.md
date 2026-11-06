# DOKUMENTASI PENGUJIAN BACKEND LOGBOOK

## 1. Tujuan Pengujian

Pengujian backend Logbook dilakukan untuk memastikan bahwa API, validasi data, logika bisnis, dan integrasi database berjalan sesuai dengan kebutuhan sistem.

Pengujian dilakukan melalui tiga pendekatan:
1. **Manual Testing:** Pengujian endpoint API menggunakan Postman.
2. **Unit Testing:** Pengujian unit kode secara terpisah, terutama DTO, service, dan perhitungan data.
3. **Integration Testing:** Pengujian interaksi antar-komponen aplikasi, endpoint API, session, dan database PostgreSQL.

## 2. Lingkungan Pengujian

| Komponen | Keterangan |
|---|---|
| Bahasa pemrograman | Java 21 |
| Framework | Spring Boot 4.0.0 |
| Build tool | Maven |
| Database | PostgreSQL 18.4 |
| Database name | logbook |
| API testing | Postman |
| Automated testing | JUnit 5, Spring Boot Test, Mockito, MockMvc |

## 3. Manual Testing API

Pengujian manual dilakukan menggunakan Postman terhadap 10 endpoint backend.

| No. | Method | Endpoint | Fungsi | Hasil |
|---|---|---|---|---|
| 1 | POST | `/api/auth/login` | Login pengguna | Berhasil |
| 2 | GET | `/api/auth/me` | Mendapatkan informasi pengguna aktif | Berhasil |
| 3 | POST | `/api/auth/logout` | Logout pengguna | Berhasil |
| 4 | GET | `/api/daily-logs?date=YYYY-MM-DD` | Mendapatkan Daily Log berdasarkan tanggal | Berhasil |
| 5 | POST | `/api/daily-logs/{dailyLogId}/tasks` | Menambahkan task pada Daily Log | Berhasil |
| 6 | PUT | `/api/tasks/{id}` | Memperbarui task | Berhasil |
| 7 | DELETE | `/api/tasks/{id}` | Menghapus task | Berhasil |
| 8 | GET | `/api/projects` | Mendapatkan daftar project pengguna | Berhasil |
| 9 | GET | `/api/tasks/master-data` | Mendapatkan data master task | Berhasil |
| 10 | GET | `/api/examples` | Mendapatkan data contoh penulisan logbook | Berhasil |

Seluruh 10 endpoint telah diuji secara manual menggunakan Postman dan memberikan hasil sesuai dengan skenario pengujian. Endpoint `POST /api/daily-logs/{dailyLogId}/submit` dan `POST /api/daily-logs/{dailyLogId}/withdraw` ditambahkan setelah pengujian manual awal dan diverifikasi melalui integration test.

## 4. Unit Testing

Unit testing dilakukan untuk menguji komponen aplikasi secara terpisah tanpa harus menjalankan keseluruhan alur API.

### 4.1 DailyLogResponseTest

Pengujian dilakukan terhadap logika penghitungan durasi pekerjaan pada Daily Log.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Menghitung durasi task yang saling tumpang tindih dalam satu project | Lulus |
| 2 | Menghitung subtotal durasi setiap project | Lulus |
| 3 | Menghindari penghitungan ganda pada task lintas project | Lulus |
| 4 | Menghitung task dengan waktu berurutan | Lulus |
| 5 | Menangani Daily Log tanpa task | Lulus |
| 6 | Menangani interval waktu identik pada beberapa project | Lulus |

Total: **6 test, seluruhnya lulus.**

### 4.2 LogTaskRequestTest

Pengujian dilakukan untuk memastikan validasi data pada request penambahan dan pembaruan task.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Request dengan data valid | Lulus |
| 2 | Project ID harus bernilai positif | Lulus |
| 3 | Task Type ID tidak boleh kosong | Lulus |
| 4 | Work Status ID tidak boleh kosong | Lulus |
| 5 | Format ticket harus sesuai ketentuan | Lulus |
| 6 | Start time tidak boleh kosong | Lulus |
| 7 | Durasi harus berada pada rentang 5-1440 menit | Lulus |
| 8 | Summary tidak boleh kosong | Lulus |

Total: **8 test, seluruhnya lulus.**

### 4.3 LogTaskServiceTest

Pengujian dilakukan untuk memastikan logika bisnis pada proses pengelolaan task.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Berhasil membuat task | Lulus |
| 2 | Daily Log tidak ditemukan | Lulus |
| 3 | Pengguna tidak memiliki akses terhadap Daily Log | Lulus |
| 4 | Task tidak dapat dibuat jika Daily Log bukan berstatus DRAF | Lulus |
| 5 | Work Status tidak valid | Lulus |
| 6 | Task Type tidak valid atau tidak aktif | Lulus |
| 7 | Durasi di luar rentang 5-1440 menit | Lulus |
| 8 | Project bukan milik pengguna | Lulus |
| 9 | Task tidak dapat dihapus jika Daily Log bukan berstatus DRAF | Lulus |
| 10 | Berhasil menghapus task | Lulus |

Total: **10 test, seluruhnya lulus.**

### 4.4 DailyLogServiceTest

Pengujian dilakukan untuk memastikan logika pembuatan dan pengiriman Daily Log.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Membuat Daily Log baru berstatus DRAF dengan work schedule default | Lulus |
| 2 | Mengembalikan Daily Log yang sudah ada tanpa membuat duplikat | Lulus |
| 3 | Mengirim Daily Log DRAF yang memiliki task | Lulus |
| 4 | Pengiriman ulang Daily Log TERKIRIM bersifat idempotent | Lulus |
| 5 | Pengiriman gagal jika Daily Log tidak memiliki task | Lulus |
| 6 | Pengiriman gagal jika Daily Log tidak ditemukan | Lulus |
| 7 | Menarik kembali Daily Log TERKIRIM menjadi DRAF | Lulus |
| 8 | Penarikan ulang Daily Log DRAF bersifat idempotent | Lulus |
| 9 | Penarikan gagal jika status Daily Log tidak dapat ditarik | Lulus |
| 10 | Penarikan gagal jika Daily Log tidak ditemukan | Lulus |

Total: **10 test, seluruhnya lulus.**

## 5. Integration Testing

Integration testing dilakukan menggunakan Spring Boot Test dan MockMvc untuk menguji interaksi antara endpoint API, komponen aplikasi, session autentikasi, dan database PostgreSQL.

Pengujian menggunakan database `logbook` dengan konfigurasi transaksi `@Transactional` untuk membantu melakukan rollback perubahan data setelah setiap test selesai.

### 5.1 Skenario Pengujian

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Login dengan email terdaftar | Lulus |
| 2 | Mendapatkan informasi pengguna aktif | Lulus |
| 3 | Logout dan memastikan session tidak lagi berlaku | Lulus |
| 4 | Mendapatkan Daily Log berdasarkan tanggal | Lulus |
| 5 | Membuat task baru | Lulus |
| 6 | Membuat, memperbarui, dan menghapus task dalam satu alur | Lulus |
| 7 | Mengirim Daily Log dan mendapatkan status TERKIRIM | Lulus |
| 8 | Menarik kembali Daily Log menjadi DRAF | Lulus |
| 9 | Mendapatkan daftar project | Lulus |
| 10 | Mendapatkan data master task | Lulus |
| 11 | Mendapatkan data contoh penulisan logbook | Lulus |
| 12 | Mengakses endpoint project tanpa login | Lulus |
| 13 | Login menggunakan email tidak terdaftar | Lulus |
| 14 | Login menggunakan email kosong | Lulus |
| 15 | Mengakses informasi pengguna tanpa login | Lulus |
| 16 | Mengakses Daily Log tanpa login | Lulus |
| 17 | Membuat task tanpa login | Lulus |
| 18 | Memperbarui task tanpa login | Lulus |
| 19 | Menghapus task tanpa login | Lulus |
| 20 | Membuat task pada Daily Log yang tidak ditemukan | Lulus |
| 21 | Memperbarui task yang tidak ditemukan | Lulus |
| 22 | Menghapus task yang tidak ditemukan | Lulus |
| 23 | Membuat task dengan Work Status tidak valid | Lulus |
| 24 | Membuat task dengan format ticket tidak valid | Lulus |
| 25 | Mengakses Daily Log tanpa parameter tanggal | Lulus |

Total: **25 test, seluruhnya lulus.**

## 6. Rekapitulasi Hasil Pengujian

Berdasarkan hasil eksekusi perintah `.\mvnw.cmd test` pada 6 November 2026, diperoleh hasil sebagai berikut.

| Komponen Pengujian | Jumlah Test | Failures | Errors | Status |
|---|---|---:|---:|---|
| DailyLogResponseTest | 6 | 0 | 0 | Lulus |
| LogTaskRequestTest | 8 | 0 | 0 | Lulus |
| LogTaskServiceTest | 10 | 0 | 0 | Lulus |
| DailyLogServiceTest | 10 | 0 | 0 | Lulus |
| LogbookApiIntegrationTest | 25 | 0 | 0 | Lulus |
| LogbookBeApplicationTests | 1 | 0 | 0 | Lulus |
| **Total** | **60** | **0** | **0** | **Lulus** |

### 6.1 Kesimpulan Hasil Pengujian

Berdasarkan pengujian yang telah dilakukan, diperoleh hasil:

- Sebanyak 10 endpoint API telah diuji secara manual menggunakan Postman, sedangkan endpoint pengiriman dan penarikan Daily Log diverifikasi melalui integration test.
- Sebanyak 60 automated test berhasil dijalankan.
- Seluruh unit test dan integration test berhasil dilewati.
- Tidak ditemukan failures maupun errors pada eksekusi pengujian otomatis.

Dengan demikian, backend Logbook berhasil melewati seluruh skenario pengujian yang telah disiapkan. Pengujian tambahan masih dapat dilakukan untuk memperluas cakupan, khususnya pada validasi kepemilikan data, pembatasan akses berdasarkan status Daily Log, serta kondisi bisnis lainnya.

## 7. Kesimpulan

Pengujian backend Logbook telah dilakukan melalui tiga pendekatan, yaitu manual testing, unit testing, dan integration testing. Pengujian manual terhadap 10 endpoint API berhasil dilakukan menggunakan Postman, endpoint pengiriman dan penarikan Daily Log diverifikasi melalui integration test, dan pengujian otomatis menghasilkan 60 test dengan 0 failures dan 0 errors.

Hasil tersebut menunjukkan bahwa komponen backend yang telah diuji berjalan sesuai dengan skenario pengujian yang tersedia. Meskipun demikian, hasil ini belum menjamin seluruh kemungkinan kondisi sistem telah diuji sehingga pengembangan cakupan pengujian tetap diperlukan.