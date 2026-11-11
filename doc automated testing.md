# DOKUMENTASI PENGUJIAN BACKEND LOGBOOK

## 1. Tujuan Pengujian

Pengujian backend Logbook dilakukan untuk memastikan bahwa API, validasi data, logika bisnis, otorisasi berbasis peran, dan integrasi database berjalan sesuai dengan kebutuhan sistem.

Pengujian dilakukan melalui tiga pendekatan:
1. **Manual Testing:** Pengujian endpoint API menggunakan Postman.
2. **Unit Testing:** Pengujian unit kode secara terpisah, terutama DTO, service, perhitungan data, dan aturan peran/otorisasi.
3. **Integration Testing:** Pengujian interaksi antar-komponen aplikasi, endpoint API, session, otorisasi, dan database PostgreSQL.

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

Saat ini backend Logbook memiliki **23 endpoint API**. Sepuluh endpoint diuji secara manual menggunakan Postman, sedangkan tiga belas endpoint yang ditambahkan setelah pengujian manual awal diverifikasi melalui integration test.

| No. | Method | Endpoint | Fungsi | Akses | Verifikasi |
|---|---|---|---|---|---|
| 1 | POST | `/api/auth/login` | Login pengguna | Publik | Postman |
| 2 | GET | `/api/auth/me` | Mendapatkan informasi pengguna aktif | Pengguna masuk | Postman |
| 3 | GET | `/api/auth/roles` | Mendapatkan peran pengguna pada permintaan saat ini | Pengguna masuk | Integration test |
| 4 | POST | `/api/auth/logout` | Logout pengguna | Pengguna masuk | Postman |
| 5 | GET | `/api/daily-logs?date=YYYY-MM-DD` | Mendapatkan Daily Log milik pengguna pada tanggal tertentu | Pemilik data | Postman |
| 6 | GET | `/api/daily-logs/users/{userId}?date=YYYY-MM-DD` | Mendapatkan Daily Log pengguna lain | Atasan (timnya), admin (semua) | Integration test |
| 7 | POST | `/api/daily-logs?date=YYYY-MM-DD` | Membuat Daily Log baru | Pemilik data | Integration test |
| 8 | POST | `/api/daily-logs/{dailyLogId}/submit` | Mengirim Daily Log untuk persetujuan | Pemilik data | Integration test |
| 9 | POST | `/api/daily-logs/{dailyLogId}/withdraw` | Menarik kembali Daily Log menjadi DRAF | Pemilik data | Integration test |
| 10 | POST | `/api/daily-logs/{dailyLogId}/approve` | Menyetujui Daily Log anggota tim | Atasan (timnya), admin (semua) | Integration test |
| 11 | POST | `/api/daily-logs/{dailyLogId}/reject` | Menolak Daily Log disertai alasan wajib | Atasan (timnya), admin (semua) | Integration test |
| 12 | POST | `/api/daily-logs/{dailyLogId}/revision-request` | Mengajukan perbaikan Daily Log yang sudah disetujui | Pemilik data | Integration test |
| 13 | POST | `/api/daily-logs/{dailyLogId}/approve-revision` | Menyetujui perbaikan dan membuka kembali hari sebagai DRAF | Atasan (timnya), admin (semua) | Integration test |
| 14 | POST | `/api/daily-logs/{dailyLogId}/tasks` | Menambahkan task pada Daily Log | Pemilik data | Postman |
| 15 | PUT | `/api/tasks/{id}` | Memperbarui task | Pemilik data | Postman |
| 16 | DELETE | `/api/tasks/{id}` | Menghapus task | Pemilik data | Postman |
| 17 | GET | `/api/team` | Mendapatkan daftar tim dengan status dan total jam hari ini | Atasan (timnya), admin (semua) | Integration test |
| 18 | GET | `/api/team/{userId}/daily-logs/{date}` | Mendapatkan Daily Log anggota tim pada tanggal tertentu | Atasan (timnya), admin (semua) | Integration test |
| 19 | POST | `/api/admin/users/import` | Mengimpor pengguna dari CSV | Admin | Integration test |
| 20 | GET | `/api/admin/daily-logs` | Mendapatkan daftar Daily Log seluruh talent dengan filter tanggal, pengguna, dan status | Admin | Integration test |
| 21 | GET | `/api/projects` | Mendapatkan daftar project pengguna | Pemilik data | Postman |
| 22 | GET | `/api/tasks/master-data` | Mendapatkan data master task | Pengguna masuk | Postman |
| 23 | GET | `/api/examples` | Mendapatkan data contoh penulisan logbook | Pengguna masuk | Postman |

Seluruh endpoint telah diverifikasi dan memberikan hasil sesuai dengan skenario pengujian.

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
| 7 | Menyertakan field `editable` dan `deadline` pada response Daily Log | Lulus |

Total: **7 test, seluruhnya lulus.**

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
| 9 | Task keempat yang tumpang tindih ditolak | Lulus |
| 10 | Dua task yang tumpang tindih tetap diterima | Lulus |
| 11 | Pembaruan task tidak menghitung task itu sendiri pada batas tumpang tindih | Lulus |
| 12 | Task tidak dapat dihapus jika Daily Log bukan berstatus DRAF | Lulus |
| 13 | Berhasil menghapus task | Lulus |
| 14 | Task tidak dapat ditambah pada hari yang sudah lewat | Lulus |
| 15 | Task tidak dapat diubah pada hari yang sudah lewat | Lulus |
| 16 | Task tidak dapat dihapus pada hari yang sudah lewat | Lulus |
| 17 | Task tidak dapat ditambah pada tanggal yang belum tiba | Lulus |
| 18 | Task tidak dapat diubah pada tanggal yang belum tiba | Lulus |
| 19 | Task tidak dapat dihapus pada tanggal yang belum tiba | Lulus |
| 20 | Task dapat ditambah pada Daily Log berstatus DITOLAK | Lulus |
| 21 | Task dapat diubah pada Daily Log berstatus DITOLAK | Lulus |
| 22 | Task dapat dihapus pada Daily Log berstatus DITOLAK | Lulus |

Total: **22 test, seluruhnya lulus.**

### 4.4 DailyLogServiceTest

Pengujian dilakukan untuk memastikan logika pembuatan dan pengiriman Daily Log.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Membuat Daily Log baru berstatus DRAF dengan work schedule default | Lulus |
| 2 | Mengembalikan Daily Log yang sudah ada tanpa membuat duplikat | Lulus |
| 3 | Mengirim Daily Log DRAF yang memiliki task | Lulus |
| 4 | Pengiriman ulang Daily Log MENUNGGU bersifat idempotent | Lulus |
| 5 | Pengiriman gagal jika Daily Log tidak memiliki task | Lulus |
| 6 | Pengiriman gagal jika Daily Log tidak ditemukan | Lulus |
| 7 | Menarik kembali Daily Log MENUNGGU menjadi DRAF | Lulus |
| 8 | Penarikan ulang Daily Log DRAF bersifat idempotent | Lulus |
| 9 | Penarikan gagal jika status Daily Log tidak dapat ditarik | Lulus |
| 10 | Penarikan gagal jika Daily Log tidak ditemukan | Lulus |
| 11 | Pembuatan Daily Log ditolak pada hari yang sudah lewat | Lulus |
| 12 | Pengiriman Daily Log ditolak pada hari yang sudah lewat | Lulus |
| 13 | Penarikan Daily Log ditolak pada hari yang sudah lewat | Lulus |
| 14 | Pembuatan Daily Log ditolak pada tanggal yang belum tiba | Lulus |
| 15 | Pengiriman Daily Log ditolak pada tanggal yang belum tiba | Lulus |
| 16 | Penarikan Daily Log ditolak pada tanggal yang belum tiba | Lulus |

Total: **16 test, seluruhnya lulus.**

### 4.5 RoleServiceTest

Pengujian dilakukan untuk memastikan peran dihitung dari data pada setiap permintaan dan aturan otorisasi berjalan benar.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Setiap pengguna selalu memiliki peran TALENT | Lulus |
| 2 | Peran ATASAN muncul dari email_atasan milik pengguna lain | Lulus |
| 3 | Peran ADMIN muncul dari flag is_admin | Lulus |
| 4 | Informasi peran menyertakan jumlah anggota tim untuk atasan | Lulus |
| 5 | Talent dapat melihat data sendiri tetapi bukan data talent lain | Lulus |
| 6 | Atasan hanya dapat melihat dan memutuskan Daily Log timnya | Lulus |
| 7 | Admin dapat melihat dan memutuskan semua Daily Log kecuali miliknya sendiri | Lulus |
| 8 | Endpoint khusus admin menolak pengguna non-admin | Lulus |
| 9 | Pembacaan data talent lain oleh talent ditolak | Lulus |
| 10 | Pengguna yang tidak ditemukan menghasilkan 404 | Lulus |
| 11 | Endpoint atasan/admin menolak talent biasa | Lulus |
| 12 | Atasan/admin tidak dapat memutuskan Daily Log miliknya sendiri | Lulus |

Total: **12 test, seluruhnya lulus.**

### 4.6 TeamServiceTest

Pengujian dilakukan untuk memastikan daftar tim, status hari ini, dan perhitungan hari menunggu berjalan benar.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Atasan hanya mendapatkan anggota timnya; status BELUM_MULAI bila belum ada Daily Log hari ini | Lulus |
| 2 | Admin mendapatkan seluruh pengguna aktif | Lulus |
| 3 | Status hari ini memakai total durasi union dan jumlah hari MENUNGGU | Lulus |
| 4 | Tim kosong mengembalikan daftar kosong | Lulus |

Total: **4 test, seluruhnya lulus.**

### 4.7 AdminDailyLogServiceTest

Pengujian dilakukan untuk memastikan daftar Daily Log admin, filter opsional, dan normalisasi status berjalan benar.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Tanpa filter, seluruh parameter null diteruskan ke repository | Lulus |
| 2 | Status dinormalisasi dan data pemilik serta total union dipetakan dengan benar | Lulus |
| 3 | Baris tetap ditampilkan meskipun data pemilik tidak ditemukan | Lulus |
| 4 | Status kosong berarti tanpa filter | Lulus |
| 5 | Status yang tidak dikenal ditolak dengan 400 | Lulus |
| 6 | Status yang dikenal diterima dalam huruf besar maupun kecil | Lulus |

Total: **6 test, seluruhnya lulus.**

### 4.8 ApprovalServiceTest

Pengujian dilakukan untuk memastikan keputusan atas Daily Log (setujui, tolak, ajukan perbaikan, setujui perbaikan), aturan alasan wajib, larangan memutuskan Daily Log sendiri, dan pencatatan log INFO berjalan benar.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Menyetujui Daily Log MENUNGGU dan mencatat decided_by serta decided_at | Lulus |
| 2 | Pelaku tidak boleh menyetujui Daily Log miliknya sendiri | Lulus |
| 3 | Pengguna yang bukan atasan pemilik ditolak | Lulus |
| 4 | Persetujuan gagal bila status bukan MENUNGGU | Lulus |
| 5 | Persetujuan gagal bila Daily Log tidak ditemukan | Lulus |
| 6 | Penolakan wajib memiliki alasan (kosong maupun null) | Lulus |
| 7 | Penolakan menyimpan alasan yang sudah di-trim dan status DITOLAK | Lulus |
| 8 | Alasan penolakan lebih dari 1000 karakter ditolak | Lulus |
| 9 | Pengajuan perbaikan hanya oleh pemilik Daily Log | Lulus |
| 10 | Pengajuan perbaikan hanya untuk Daily Log berstatus DISETUJUI | Lulus |
| 11 | Pengajuan perbaikan menyimpan alasan dan status PERBAIKAN_DIAJUKAN | Lulus |
| 12 | Persetujuan perbaikan membuka kembali hari sebagai DRAF | Lulus |
| 13 | Persetujuan perbaikan hanya untuk status PERBAIKAN_DIAJUKAN | Lulus |
| 14 | Persetujuan perbaikan tidak boleh untuk Daily Log sendiri | Lulus |
| 15 | Setiap keputusan menulis satu log INFO berisi pelaku dan tanggal Daily Log | Lulus |

Total: **15 test, seluruhnya lulus.**

### 4.9 DeadlineServiceTest

Pengujian dilakukan untuk memastikan perhitungan batas hari, penentuan hari ini, dan aturan hari yang dapat diisi berjalan benar.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Deadline adalah pukul 00:00 hari berikutnya pada zona waktu yang dikonfigurasi | Lulus |
| 2 | Tanggal kemarin sudah melewati deadline | Lulus |
| 3 | Tanggal hari ini dan besok belum melewati deadline | Lulus |
| 4 | Hanya tanggal besok yang dianggap hari yang belum tiba | Lulus |
| 5 | Hanya tanggal hari ini yang dianggap hari ini | Lulus |
| 6 | Daily Log DRAF hari ini dapat disunting | Lulus |
| 7 | Daily Log DRAF kemarin tidak dapat disunting | Lulus |
| 8 | Daily Log DRAF besok tidak dapat disunting | Lulus |
| 9 | Status selain DRAF tidak dapat disunting | Lulus |
| 10 | Saat aturan dimatikan, hari yang sudah lewat tetap dapat disunting | Lulus |
| 11 | Saat aturan dimatikan, hari yang belum tiba tetap dapat disunting | Lulus |
| 12 | Daily Log DITOLAK hari ini dapat disunting untuk diperbaiki | Lulus |
| 13 | Daily Log DITOLAK pada hari lain tidak dapat disunting | Lulus |

Total: **13 test, seluruhnya lulus.**

## 5. Integration Testing

Integration testing dilakukan menggunakan Spring Boot Test dan MockMvc untuk menguji interaksi antara endpoint API, komponen aplikasi, session autentikasi, otorisasi berbasis peran, dan database PostgreSQL.

Pengujian menggunakan database `logbook` dengan konfigurasi transaksi `@Transactional` untuk membantu melakukan rollback perubahan data setelah setiap test selesai. Terdapat dua kelas integration test dengan konfigurasi aturan batas hari yang berbeda agar keduanya dapat berjalan di database lokal yang sama.

### 5.1 LogbookApiIntegrationTest

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Login dengan email terdaftar | Lulus |
| 2 | Mendapatkan informasi pengguna aktif | Lulus |
| 3 | Logout dan memastikan session tidak lagi berlaku | Lulus |
| 4 | Login menggunakan email tidak terdaftar | Lulus |
| 5 | Login menggunakan email kosong | Lulus |
| 6 | Mendapatkan Daily Log berdasarkan tanggal | Lulus |
| 7 | Mengirim Daily Log dan mendapatkan status MENUNGGU | Lulus |
| 8 | Menarik kembali Daily Log menjadi DRAF | Lulus |
| 9 | Membuat task baru | Lulus |
| 10 | Membuat, memperbarui, dan menghapus task dalam satu alur | Lulus |
| 11 | Mendapatkan daftar project | Lulus |
| 12 | Mendapatkan data master task | Lulus |
| 13 | Mendapatkan data contoh penulisan logbook | Lulus |
| 14 | Mengakses endpoint project tanpa login | Lulus |
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
| 25 | Membuat task keempat yang tumpang tindih ditolak | Lulus |
| 26 | Mengakses Daily Log tanpa parameter tanggal | Lulus |
| 27 | Peran pengguna biasa hanya TALENT | Lulus |
| 28 | Informasi pengguna aktif menyertakan daftar peran | Lulus |
| 29 | Mengakses informasi peran tanpa login | Lulus |
| 30 | Peran ATASAN muncul saat email_atasan pengguna lain menunjuk ke pengguna tersebut | Lulus |
| 31 | Peran ADMIN muncul saat flag is_admin aktif | Lulus |
| 32 | Talent tidak dapat membaca Daily Log talent lain | Lulus |
| 33 | Atasan dapat membaca Daily Log anggota timnya | Lulus |
| 34 | Atasan tidak dapat membaca Daily Log di luar timnya | Lulus |
| 35 | Admin dapat membaca Daily Log talent mana pun | Lulus |
| 36 | Non-admin tidak dapat mengimpor pengguna | Lulus |
| 37 | Admin dapat mengimpor pengguna | Lulus |
| 38 | Talent tidak dapat mengakses daftar tim | Lulus |
| 39 | Atasan hanya melihat anggota timnya pada daftar tim | Lulus |
| 40 | Daftar tim menampilkan status hari ini, total union, dan jumlah hari MENUNGGU | Lulus |
| 41 | Admin melihat seluruh pengguna pada daftar tim | Lulus |
| 42 | Atasan dapat membaca Daily Log tim melalui endpoint tim | Lulus |
| 43 | Atasan tidak dapat membaca Daily Log di luar tim melalui endpoint tim | Lulus |
| 44 | Talent tidak dapat memakai endpoint Daily Log tim | Lulus |
| 45 | Admin dapat membaca Daily Log mana pun melalui endpoint tim | Lulus |
| 46 | Talent tidak dapat mengakses daftar Daily Log admin | Lulus |
| 47 | Admin mendapatkan daftar Daily Log seluruh talent | Lulus |
| 48 | Admin dapat memfilter daftar Daily Log berdasarkan tanggal | Lulus |
| 49 | Admin dapat memfilter daftar Daily Log berdasarkan pengguna | Lulus |
| 50 | Admin dapat memfilter daftar Daily Log berdasarkan status | Lulus |
| 51 | Filter status admin tidak peka huruf besar/kecil | Lulus |
| 52 | Admin dapat menggabungkan filter tanggal, pengguna, dan status | Lulus |
| 53 | Filter status yang tidak valid menghasilkan 400 | Lulus |
| 54 | Filter tanggal yang tidak valid menghasilkan 400 | Lulus |
| 55 | Filter pengguna yang tidak valid menghasilkan 400 | Lulus |
| 56 | Atasan dapat menyetujui Daily Log anggota timnya dan keputusan tersimpan | Lulus |
| 57 | Atasan dapat menolak Daily Log anggota timnya disertai alasan | Lulus |
| 58 | Penolakan tanpa alasan menghasilkan 400 | Lulus |
| 59 | Tidak ada yang boleh memutuskan Daily Log miliknya sendiri | Lulus |
| 60 | Talent tidak dapat menyetujui Daily Log talent lain | Lulus |
| 61 | Atasan tidak dapat menyetujui Daily Log di luar timnya | Lulus |
| 62 | Persetujuan hanya untuk Daily Log berstatus MENUNGGU | Lulus |
| 63 | Persetujuan Daily Log yang tidak ditemukan menghasilkan 404 | Lulus |
| 64 | Menyetujui Daily Log tanpa login menghasilkan 401 | Lulus |
| 65 | Talent dapat mengajukan perbaikan atas Daily Log miliknya yang disetujui | Lulus |
| 66 | Pengajuan perbaikan tanpa alasan menghasilkan 400 | Lulus |
| 67 | Pengajuan perbaikan hanya untuk Daily Log berstatus DISETUJUI | Lulus |
| 68 | Talent tidak dapat mengajukan perbaikan atas Daily Log talent lain | Lulus |
| 69 | Atasan dapat menyetujui perbaikan sehingga hari dibuka kembali sebagai DRAF | Lulus |
| 70 | Persetujuan perbaikan hanya untuk status PERBAIKAN_DIAJUKAN | Lulus |
| 71 | Daily Log DITOLAK dapat diperbaiki dan dikirim ulang menjadi MENUNGGU | Lulus |

Total: **71 test, seluruhnya lulus.**

### 5.2 DailyLogDeadlineIntegrationTest

Pengujian ini dijalankan dengan `logbook.batas-hari.aktif=true` untuk memastikan aturan batas hari benar-benar ditegakkan pada level API, termasuk ketika data uji bertanggal lampau. Sebaliknya, `LogbookApiIntegrationTest` dijalankan dengan aturan dimatikan agar fixture bertanggal lampau tetap dapat dipakai.

| No. | Skenario Pengujian | Hasil |
|---|---|---|
| 1 | Response hari yang sudah lewat menyertakan `editable=false` dan `deadline`, dan task baru ditolak | Lulus |
| 2 | Hari yang sudah lewat menolak pengiriman Daily Log | Lulus |
| 3 | Hari yang sudah lewat menolak penarikan Daily Log | Lulus |
| 4 | Hari yang sudah lewat menolak pembaruan task | Lulus |
| 5 | Hari yang sudah lewat menolak penghapusan task | Lulus |
| 6 | Pembuatan Daily Log baru ditolak pada hari yang sudah lewat | Lulus |
| 7 | Hari ini tetap dapat disunting dan menerima task baru | Lulus |
| 8 | Tanggal besok terkunci, task baru dan pengiriman ditolak, `editable=false` | Lulus |
| 9 | Pembuatan Daily Log baru ditolak pada tanggal yang belum tiba | Lulus |
| 10 | Response hari ini menyertakan `editable=true` dan `deadline` | Lulus |
| 11 | Daily Log DITOLAK hari ini dapat diperbaiki dan dikirim ulang, alasan penolakan dibersihkan | Lulus |
| 12 | Daily Log DITOLAK pada hari yang sudah lewat tetap terkunci | Lulus |

Total: **12 test, seluruhnya lulus.**

## 6. Rekapitulasi Hasil Pengujian

Berdasarkan hasil eksekusi `.\mvnw.cmd test "-Dtest=!LogbookApiIntegrationTest,!DailyLogDeadlineIntegrationTest"` untuk unit test dan `.\mvnw.cmd test "-Dtest=LogbookApiIntegrationTest,DailyLogDeadlineIntegrationTest"` untuk integration test pada 11 November 2026, diperoleh hasil sebagai berikut.

| Komponen Pengujian | Jumlah Test | Failures | Errors | Status |
|---|---|---:|---:|---|
| DailyLogResponseTest | 7 | 0 | 0 | Lulus |
| LogTaskRequestTest | 8 | 0 | 0 | Lulus |
| LogTaskServiceTest | 22 | 0 | 0 | Lulus |
| DailyLogServiceTest | 16 | 0 | 0 | Lulus |
| DeadlineServiceTest | 13 | 0 | 0 | Lulus |
| RoleServiceTest | 12 | 0 | 0 | Lulus |
| TeamServiceTest | 4 | 0 | 0 | Lulus |
| AdminDailyLogServiceTest | 6 | 0 | 0 | Lulus |
| ApprovalServiceTest | 15 | 0 | 0 | Lulus |
| LogbookApiIntegrationTest | 71 | 0 | 0 | Lulus |
| DailyLogDeadlineIntegrationTest | 12 | 0 | 0 | Lulus |
| LogbookBeApplicationTests | 1 | 0 | 0 | Lulus |
| **Total** | **187** | **0** | **0** | **Lulus** |

### 6.1 Kesimpulan Hasil Pengujian

Berdasarkan pengujian yang telah dilakukan, diperoleh hasil:

- Sebanyak 23 endpoint API tersedia; 10 endpoint diuji secara manual menggunakan Postman dan 13 endpoint diverifikasi melalui integration test.
- Sebanyak 187 automated test berhasil dijalankan.
- Seluruh unit test dan integration test berhasil dilewati.
- Tidak ditemukan failures maupun errors pada eksekusi pengujian otomatis.
- Otorisasi berbasis peran (talent, atasan, admin) telah diuji pada level unit maupun integrasi.
- Alur persetujuan Daily Log (setujui, tolak, ajukan perbaikan, setujui perbaikan), aturan alasan wajib, larangan memutuskan Daily Log sendiri, dan pencatatan log INFO setiap keputusan telah diuji.
- Aturan batas hari telah diuji: hanya Daily Log tanggal hari ini yang dapat ditambah, diubah, atau dikirim, sedangkan hari yang sudah lewat maupun yang belum tiba ditolak dengan status 409.
- Alur Daily Log DITOLAK telah diuji: pada hari yang sama task masih dapat diperbaiki lalu Daily Log dikirim ulang, sedangkan pada hari berikutnya hari tersebut tetap terkunci.

Dengan demikian, backend Logbook berhasil melewati seluruh skenario pengujian yang telah disiapkan. Pengujian tambahan masih dapat dilakukan untuk memperluas cakupan, khususnya pada pengelolaan hari libur dan cuti, pengiriman otomatis di akhir hari, serta kondisi bisnis lainnya.

## 7. Kesimpulan

Pengujian backend Logbook telah dilakukan melalui tiga pendekatan, yaitu manual testing, unit testing, dan integration testing. Pengujian manual terhadap 10 endpoint API berhasil dilakukan menggunakan Postman, 13 endpoint tambahan diverifikasi melalui integration test, dan pengujian otomatis menghasilkan 187 test dengan 0 failures dan 0 errors.

Hasil tersebut menunjukkan bahwa komponen backend yang telah diuji berjalan sesuai dengan skenario pengujian yang tersedia. Meskipun demikian, hasil ini belum menjamin seluruh kemungkinan kondisi sistem telah diuji sehingga pengembangan cakupan pengujian tetap diperlukan.
