# Hostel Management System

A full-stack Hostel Management System built with **Spring Boot 3 (Java 17)**, **Spring Security + JWT**, **MySQL**, and a vanilla HTML/CSS/JS frontend.

## Features

- **Two roles:** Student and Administrator, each with a separate dashboard and permissions.
- **Secure authentication:** login with email *or* mobile number, plus password — all validated on both the browser and the server.
- **Encrypted passwords:** every password is hashed with **BCrypt (strength 12)** before it is stored. Plain-text passwords are never saved, logged, or compared directly.
- **JWT sessions:** stateless authentication using signed JSON Web Tokens.
- **Room management (Admin):** add, update, delete rooms; track capacity and occupancy automatically.
- **Room booking (Student):** browse available rooms and submit booking requests.
- **Booking approval workflow (Admin):** approve, reject, cancel, or complete bookings; room occupancy updates automatically.
- **Payments:** students pay rent for an approved booking (simulated gateway — Card / UPI / Net banking / Cash); admins can view all transactions.
- **Input validation:**
  - Email must match a valid email format.
  - Mobile number must be a valid 10-digit number (starting 6-9).
  - Password must be 8-20 characters with at least one uppercase letter, one lowercase letter, one digit, and one special character (`@#$%^&+=!`).

---

## Tech Stack

| Layer      | Technology |
|------------|------------|
| Backend    | Java 17, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Spring Security |
| Auth       | JWT (jjwt), BCryptPasswordEncoder |
| Database   | MySQL 8 (H2 in-memory option for quick testing) |
| Frontend   | HTML5, CSS3, vanilla JavaScript (served as static files by Spring Boot) |
| Build tool | Maven |

---

## Project Structure

```
hostel-management-system/
├── pom.xml
├── README.md
├── .gitignore
└── src/main/
    ├── java/com/hostel/management/
    │   ├── HostelManagementApplication.java
    │   ├── config/          (SecurityConfig, DataSeeder)
    │   ├── model/            (User, Role, Room, Booking, Payment)
    │   ├── repository/       (Spring Data JPA repositories)
    │   ├── dto/               (Request/response objects with validation)
    │   ├── service/          (Business logic)
    │   ├── controller/       (REST endpoints)
    │   ├── security/         (JWT filter, JwtUtil, UserDetails)
    │   └── exception/        (Global error handling)
    └── resources/
        ├── application.properties
        └── static/
            ├── index.html
            ├── css/style.css
            ├── js/api.js
            └── pages/ (login.html, register.html, student-dashboard.html, admin-dashboard.html)
```

---

## Part 1 — Prerequisites

Install these before you start:

1. **Java Development Kit (JDK) 17 or later**
   Check with: `java -version`
2. **Maven** (optional — IntelliJ has Maven built in, and a Maven wrapper isn't required)
3. **MySQL Server 8.x** (Community edition is fine) — or skip this and use the built-in H2 database for a quick test run (see Part 4).
4. **IntelliJ IDEA** (Community or Ultimate edition)
5. **Git** installed and a **GitHub account**

---

## Part 2 — Set Up MySQL

1. Open MySQL Workbench, or your terminal / MySQL shell, and log in:
   ```bash
   mysql -u root -p
   ```
2. Create the database (the app can also auto-create it, but it's good practice to do it yourself):
   ```sql
   CREATE DATABASE hostel_management_db;
   ```
3. Note your MySQL **username** and **password** — you'll put these into `application.properties` in Part 4.

---

## Part 3 — Open the Project in IntelliJ IDEA

1. **Extract** the project zip you downloaded to a folder on your computer, e.g. `C:\projects\hostel-management-system` or `~/projects/hostel-management-system`.
2. Open **IntelliJ IDEA**.
3. On the Welcome screen, click **Open** (or `File → Open` if a project is already open).
4. Select the `hostel-management-system` folder (the one containing `pom.xml`) and click **OK**.
5. IntelliJ will detect it as a **Maven project** and show a notification to "Load Maven Project" — click it (or IntelliJ will auto-import). This downloads all dependencies listed in `pom.xml`; it may take a minute or two the first time.
6. Once indexing/import finishes, confirm the Project SDK is Java 17:
   - `File → Project Structure → Project` → set **SDK** to a JDK 17 installation (click "Add SDK" if you don't have one listed, and point it at your JDK 17 install folder).
   - Also check `File → Project Structure → Modules` uses language level 17.

---

## Part 4 — Configure the Database Connection

1. In the Project panel, open:
   `src/main/resources/application.properties`
2. Update these two lines with your actual MySQL credentials:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```
3. The rest of the config points to `jdbc:mysql://localhost:3306/hostel_management_db` and will auto-create tables (`spring.jpa.hibernate.ddl-auto=update`), so you don't need to write any SQL for tables.

**Don't want to install MySQL right now?** Comment out the MySQL block in `application.properties` and uncomment the H2 block below it (instructions are inside the file). The app will then run entirely in memory — perfect for a quick test, but data resets every restart.

---

## Part 5 — Run the Application in IntelliJ

1. In the Project panel, navigate to:
   `src/main/java/com/hostel/management/HostelManagementApplication.java`
2. Right-click the file → **Run 'HostelManagementApplication'** (or click the green ▶ play icon next to the `main` method / class name).
3. Watch the **Run** console at the bottom. You should see Spring Boot's startup banner, then a line like:
   ```
   Tomcat started on port(s): 8080
   ======================================================
    Default admin created:
    Email: admin@hostel.com | Password: Admin@123
   ======================================================
   Started HostelManagementApplication in X.XXX seconds
   ```
   The app **automatically creates a default admin account** on first run (email `admin@hostel.com`, password `Admin@123`) so you can log in immediately as an administrator. Change this password after logging in for the first time in a real deployment.

4. Open your browser and go to:
   ```
   http://localhost:8080
   ```
   You'll be redirected to the login page.

### Try it out
- **As admin:** log in with `admin@hostel.com` / `Admin@123` → add a few rooms → wait for student bookings → approve/reject them.
- **As a student:** go to `http://localhost:8080/pages/register.html`, create a Student account, browse rooms, and submit a booking request. Once the admin approves it, come back and click "Pay rent" to simulate a payment.

---

## Part 6 — Push the Project to GitHub

If the project isn't already a Git repository (check for a `.git` folder), initialize one:

1. Open the **Terminal** tab inside IntelliJ (`View → Tool Windows → Terminal`), or use your OS terminal, `cd` into the project folder.
2. Initialize Git and make your first commit:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: Hostel Management System"
   ```
3. Create a new, **empty** repository on GitHub:
   - Go to https://github.com/new
   - Give it a name, e.g. `hostel-management-system`
   - **Do not** check "Add a README" or "Add .gitignore" (you already have both) — this avoids merge conflicts.
   - Click **Create repository**.
4. GitHub will show you the remote URL. Connect your local repo to it and push:
   ```bash
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/hostel-management-system.git
   git push -u origin main
   ```
   Replace `YOUR_USERNAME` with your actual GitHub username. If prompted, sign in with your GitHub credentials or a Personal Access Token (GitHub no longer accepts plain passwords over HTTPS for pushes — use a token from `Settings → Developer settings → Personal access tokens`, or use `git` with SSH keys instead).

### Doing this directly from IntelliJ (alternative to the terminal)
1. `VCS → Create Git Repository` (if not already a repo) and select the project root.
2. `VCS → Commit` (or `Ctrl+K` / `Cmd+K`) → select all files → write a commit message → **Commit**.
3. `VCS → Git → Push` (or `Ctrl+Shift+K` / `Cmd+Shift+K`).
4. The first time, IntelliJ will ask you to define a remote — paste your GitHub repository URL there, then push.
5. You may need to sign into GitHub inside IntelliJ (`File → Settings → Version Control → GitHub` → Add account) if it asks.

### Making future changes
```bash
git add .
git commit -m "Describe what you changed"
git push
```

---

## API Reference (quick overview)

All endpoints are prefixed with `/api`.

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/auth/register` | Public | Register a new student or admin |
| POST | `/auth/login` | Public | Log in with email/mobile + password, returns a JWT |
| GET | `/rooms` | Public | List all rooms |
| POST | `/rooms` | Admin | Add a room |
| PUT | `/rooms/{id}` | Admin | Update a room |
| DELETE | `/rooms/{id}` | Admin | Delete a room |
| POST | `/bookings` | Student | Request a room booking |
| GET | `/bookings/my` | Student | View your own bookings |
| GET | `/bookings` | Admin | View all booking requests |
| PUT | `/bookings/{id}/status` | Admin | Approve / reject / cancel / complete a booking |
| POST | `/payments` | Student | Pay rent for an approved booking |
| GET | `/payments/my` | Student | View your payment history |
| GET | `/payments` | Admin | View all payments |
| GET | `/profile/me` | Any logged-in user | Get your own profile |

Send the JWT returned from login/register in the `Authorization: Bearer <token>` header for all protected endpoints.

---

## Security Notes

- Passwords are hashed with **BCrypt** (never stored or transmitted in plain text after registration).
- Authentication is **stateless** — the server issues a signed JWT; no session state is kept server-side.
- **Before deploying to production:**
  - Change `app.jwt.secret` in `application.properties` to a long, random value, and load it from an environment variable instead of committing it to Git.
  - Change the default admin password immediately.
  - Serve the app over HTTPS.
  - Integrate a real payment gateway (Razorpay/Stripe/PayPal) instead of the simulated payment in `PaymentService`.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Communications link failure` on startup | MySQL isn't running, or the port/credentials in `application.properties` are wrong. Start MySQL and double check the username/password. |
| `Access denied for user 'root'@'localhost'` | Your MySQL password in `application.properties` doesn't match your actual MySQL root password. |
| Port 8080 already in use | Change `server.port` in `application.properties` to something else, e.g. `8081`. |
| Maven dependencies not downloading | Check your internet connection, then `File → Reload Maven Project` in IntelliJ (right-click `pom.xml` → Maven → Reload project). |
| 403 Forbidden on API calls | You're either not logged in, or logged in with the wrong role for that endpoint (e.g. a student calling an admin-only endpoint). |
