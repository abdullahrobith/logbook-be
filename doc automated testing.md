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

Seluruh 10 endpoint telah diuji secara manual menggunakan Postman dan memberikan hasil sesuai dengan skenario pengujian.

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
| 7 | Durasi harus bernilai positif | Lulus |
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
| 6 | Project bukan milik pengguna | Lulus |
| 7 | Task tidak dapat dihapus jika Daily Log bukan berstatus DRAF | Lulus |
| 8 | Berhasil menghapus task | Lulus |

Total: **8 test, seluruhnya lulus.**

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
| 7 | Mendapatkan daftar project | Lulus |
| 8 | Mendapatkan data master task | Lulus |
| 9 | Mendapatkan data contoh penulisan logbook | Lulus |
| 10 | Mengakses endpoint project tanpa login | Lulus |
| 11 | Login menggunakan email tidak terdaftar | Lulus |
| 12 | Login menggunakan email kosong | Lulus |
| 13 | Mengakses informasi pengguna tanpa login | Lulus |
| 14 | Mengakses Daily Log tanpa login | Lulus |
| 15 | Membuat task tanpa login | Lulus |
| 16 | Memperbarui task tanpa login | Lulus |
| 17 | Menghapus task tanpa login | Lulus |
| 18 | Membuat task pada Daily Log yang tidak ditemukan | Lulus |
| 19 | Memperbarui task yang tidak ditemukan | Lulus |
| 20 | Menghapus task yang tidak ditemukan | Lulus |
| 21 | Membuat task dengan Work Status tidak valid | Lulus |
| 22 | Membuat task dengan format ticket tidak valid | Lulus |
| 23 | Mengakses Daily Log tanpa parameter tanggal | Lulus |

Total: **23 test, seluruhnya lulus.**

## 6. Rekapitulasi Hasil Pengujian

Berdasarkan hasil eksekusi perintah `.\mvnw.cmd clean test` pada 4 November 2026, diperoleh hasil sebagai berikut.

| Komponen Pengujian | Jumlah Test | Failures | Errors | Status |
|---|---:|---:|---:|---|
| DailyLogResponseTest | 6 | 0 | 0 | Lulus |
| LogTaskRequestTest | 8 | 0 | 0 | Lulus |
| LogTaskServiceTest | 8 | 0 | 0 | Lulus |
| LogbookApiIntegrationTest | 23 | 0 | 0 | Lulus |
| LogbookBeApplicationTests | 1 | 0 | 0 | Lulus |
| **Total** | **46** | **0** | **0** | **Lulus** |

### 6.1 Kesimpulan Hasil Pengujian

Berdasarkan pengujian yang telah dilakukan, diperoleh hasil:

- Sebanyak 10 endpoint API telah diuji secara manual menggunakan Postman.
- Sebanyak 46 automated test berhasil dijalankan.
- Seluruh unit test dan integration test berhasil dilewati.
- Tidak ditemukan failures maupun errors pada eksekusi pengujian otomatis.

Dengan demikian, backend Logbook berhasil melewati seluruh skenario pengujian yang telah disiapkan. Pengujian tambahan masih dapat dilakukan untuk memperluas cakupan, khususnya pada validasi kepemilikan data, pembatasan akses berdasarkan status Daily Log, serta kondisi bisnis lainnya.

## 7. Kesimpulan

Pengujian backend Logbook telah dilakukan melalui tiga pendekatan, yaitu manual testing, unit testing, dan integration testing. Pengujian manual terhadap 10 endpoint API berhasil dilakukan menggunakan Postman, sedangkan pengujian otomatis menghasilkan 46 test dengan 0 failures dan 0 errors.

Hasil tersebut menunjukkan bahwa komponen backend yang telah diuji berjalan sesuai dengan skenario pengujian yang tersedia. Meskipun demikian, hasil ini belum menjamin seluruh kemungkinan kondisi sistem telah diuji sehingga pengembangan cakupan pengujian tetap diperlukan.