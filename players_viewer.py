#!/usr/bin/env python3
"""Administrative PostgreSQL browser for PryanikPlugin."""
from __future__ import annotations

import tkinter as tk
from tkinter import messagebox, ttk
from typing import Any

try:
    import psycopg
except ImportError:
    psycopg = None

# Exact data tables created by Database.java.  This allowlist deliberately excludes
# PostgreSQL schemas, system tables and anything the plugin does not own.
PLUGIN_TABLES = (
    "player_reports",
    "player_activity",
    "player_homes",
    "clan_invites",
    "clan_members",
    "clans",
    "tp_blocks",
    "player_positions",
    "hardcore_bans",
    "game_instances",
    "plugin_world_sets",
    "plugin_worlds",
    "plugin_schema_migrations",
    "players",
)
PLAYER_COLUMNS = (
    ("nickname", "Ник", 140),
    ("uuid", "UUID", 270),
    ("rank", "Ранг", 100),
    ("register_code", "Код регистрации", 120),
    ("home_world", "Дом · мир", 130),
    ("home_x", "Дом X", 75),
    ("home_y", "Дом Y", 75),
    ("home_z", "Дом Z", 75),
    ("home_yaw", "Дом yaw", 75),
    ("home_pitch", "Дом pitch", 75),
    ("last_seen", "Последний вход", 155),
    ("clan", "Клан", 125),
    ("clan_instance", "Инстанс клана", 130),
    ("saved_world", "Сохранённый мир", 140),
    ("saved_x", "Сохранённый X", 85),
    ("saved_y", "Сохранённый Y", 85),
    ("saved_z", "Сохранённый Z", 85),
    ("saved_yaw", "Сохранённый yaw", 85),
    ("saved_pitch", "Сохранённый pitch", 85),
    ("position_updated", "Позиция обновлена", 155),
    ("hardcore_instances", "Hardcore-баны", 150),
    ("tp_blocks", "Блокировки ТП", 105),
    ("reports_received", "Жалобы на игрока", 115),
    ("reports_sent", "Отправлено жалоб", 115),
)


class PlayersViewer(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("PryanikPlugin · Центр данных")
        self.geometry("1380x780")
        self.minsize(980, 580)
        self.configure(bg="#f2f5f9")
        self._style()
        self._build_ui()

    def _style(self) -> None:
        style = ttk.Style(self)
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass
        style.configure("App.TFrame", background="#f2f5f9")
        style.configure("Card.TFrame", background="#ffffff")
        style.configure("Title.TLabel", background="#f2f5f9", foreground="#172033", font=("Segoe UI", 19, "bold"))
        style.configure("Sub.TLabel", background="#f2f5f9", foreground="#65748b", font=("Segoe UI", 10))
        style.configure("Card.TLabel", background="#ffffff", foreground="#243047", font=("Segoe UI", 9))
        style.configure("Treeview", rowheight=29, font=("Segoe UI", 9), background="#ffffff", fieldbackground="#ffffff")
        style.configure("Treeview.Heading", font=("Segoe UI", 9, "bold"), background="#e9eef5", foreground="#26354c")
        style.map("Treeview", background=[("selected", "#cfe3ff")], foreground=[("selected", "#132542")])
        style.configure("TButton", padding=(10, 6))
        style.configure("Danger.TButton", foreground="#a32232")

    def _build_ui(self) -> None:
        root = ttk.Frame(self, style="App.TFrame", padding=(18, 14))
        root.pack(fill="both", expand=True)
        ttk.Label(root, text="PryanikPlugin · Центр данных", style="Title.TLabel").pack(anchor="w")
        ttk.Label(root, text="Игроки, кланы, позиции, блокировки, жалобы и другие таблицы PostgreSQL", style="Sub.TLabel").pack(anchor="w", pady=(2, 14))

        card = ttk.LabelFrame(root, text="Подключение к PostgreSQL", padding=10)
        card.pack(fill="x", pady=(0, 10))
        self.fields: dict[str, ttk.Entry] = {}
        specs = [("host", "Хост", "127.0.0.1"), ("port", "Порт", "5432"),
                 ("dbname", "База данных", "Pryanik-Plugin"), ("user", "Пользователь", "postgres"), ("password", "Пароль", "")]
        for col, (key, caption, default) in enumerate(specs):
            box = ttk.Frame(card, style="Card.TFrame")
            box.grid(row=0, column=col, sticky="ew", padx=5, pady=(0, 8))
            ttk.Label(box, text=caption, style="Card.TLabel").pack(anchor="w", pady=(0, 3))
            entry = ttk.Entry(box, show="*" if key == "password" else "")
            entry.insert(0, default)
            entry.pack(fill="x")
            self.fields[key] = entry
            card.columnconfigure(col, weight=1)
        actions = ttk.Frame(card, style="Card.TFrame")
        actions.grid(row=1, column=0, columnspan=5, sticky="ew", padx=4)
        self.refresh_btn = ttk.Button(actions, text="⟳  Подключиться / обновить", command=self.load_players)
        self.refresh_btn.pack(side="left", padx=(0, 7))
        ttk.Button(actions, text="Жалобы", command=self.show_reports).pack(side="left", padx=3)
        ttk.Button(actions, text="Дома", command=self.show_homes).pack(side="left", padx=3)
        ttk.Button(actions, text="Обзор таблиц", command=self.show_table_overview).pack(side="left", padx=3)
        ttk.Button(actions, text="Удалить игрока", command=self.delete_selected).pack(side="left", padx=3)
        ttk.Button(actions, text="Экспорт игроков CSV", command=self.export_csv).pack(side="left", padx=3)
        ttk.Button(actions, text="Очистить данные плагина…", style="Danger.TButton", command=self.confirm_wipe).pack(side="right")

        toolbar = ttk.Frame(root, style="App.TFrame")
        toolbar.pack(fill="x", pady=(1, 7))
        self.count_label = ttk.Label(toolbar, text="Игроков: 0", style="Sub.TLabel")
        self.count_label.pack(side="left")
        self.tables_label = ttk.Label(toolbar, text="Таблицы: —", style="Sub.TLabel")
        self.tables_label.pack(side="left", padx=(18, 0))
        self.status = ttk.Label(toolbar, text="Укажите параметры подключения", style="Sub.TLabel")
        self.status.pack(side="right")

        table_frame = ttk.Frame(root, style="Card.TFrame")
        table_frame.pack(fill="both", expand=True)
        self.tree = ttk.Treeview(table_frame, columns=tuple(c[0] for c in PLAYER_COLUMNS), show="headings", selectmode="browse")
        for key, title, width in PLAYER_COLUMNS:
            self.tree.heading(key, text=title, command=lambda c=key: self._sort_column(c, False))
            self.tree.column(key, width=width, minwidth=65, anchor="w", stretch=False)
        yscroll = ttk.Scrollbar(table_frame, orient="vertical", command=self.tree.yview)
        xscroll = ttk.Scrollbar(table_frame, orient="horizontal", command=self.tree.xview)
        self.tree.configure(yscrollcommand=yscroll.set, xscrollcommand=xscroll.set)
        self.tree.grid(row=0, column=0, sticky="nsew")
        yscroll.grid(row=0, column=1, sticky="ns")
        xscroll.grid(row=1, column=0, sticky="ew")
        table_frame.rowconfigure(0, weight=1)
        table_frame.columnconfigure(0, weight=1)
        self.tree.bind("<Double-1>", lambda _event: self.delete_selected())
        ttk.Label(root, text="Хеши паролей не отображаются. Очистка затрагивает только перечисленные таблицы PryanikPlugin; структура БД и посторонние таблицы сохраняются.", style="Sub.TLabel", wraplength=1250).pack(anchor="w", pady=(8, 0))

    def _connection(self):
        if psycopg is None:
            raise RuntimeError("Не установлен psycopg. Выполните: python -m pip install -r requirements-viewer.txt")
        port = int(self.fields["port"].get().strip())
        if not 1 <= port <= 65535:
            raise ValueError("Порт должен быть от 1 до 65535")
        return psycopg.connect(host=self.fields["host"].get().strip(), port=port,
                               dbname=self.fields["dbname"].get().strip(), user=self.fields["user"].get().strip(),
                               password=self.fields["password"].get(), connect_timeout=8,
                               application_name="PryanikPlugin Players Viewer")

    def _show_error(self, title: str, exc: Exception) -> None:
        self.status.configure(text="Ошибка")
        messagebox.showerror(title, f"{exc}\n\nПроверьте PostgreSQL, права пользователя, параметры подключения и наличие таблиц PryanikPlugin.")

    def _sort_column(self, column: str, reverse: bool) -> None:
        rows = [(self.tree.set(item, column), item) for item in self.tree.get_children("")]
        def sort_key(pair: tuple[str, str]) -> tuple[int, Any]:
            value = pair[0]
            try:
                return (0, float(value))
            except (ValueError, TypeError):
                return (1, value.casefold())
        for index, (_, item) in enumerate(sorted(rows, key=sort_key, reverse=reverse)):
            self.tree.move(item, "", index)
        self.tree.heading(column, command=lambda: self._sort_column(column, not reverse))

    def load_players(self) -> None:
        self.refresh_btn.configure(state="disabled")
        self.status.configure(text="Загрузка данных…")
        self.update_idletasks()
        try:
            with self._connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("""
                        SELECT p.nickname, p.uuid::text, p.rank, p.register_code,
                               p.home_world, p.home_x, p.home_y, p.home_z, p.home_yaw, p.home_pitch,
                               a.last_seen,
                               c.name, c.instance_id,
                               pos.world_name, pos.x, pos.y, pos.z, pos.yaw, pos.pitch, pos.updated_at,
                               COALESCE(hb.total, 0), COALESCE(tb.total, 0),
                               COALESCE(rt.total, 0), COALESCE(rs.total, 0)
                        FROM players p
                        LEFT JOIN player_activity a ON a.player_uuid = p.uuid
                        LEFT JOIN LATERAL (
                            SELECT c.name, c.instance_id FROM clans c
                            LEFT JOIN clan_members cm ON cm.clan_id = c.id
                            WHERE c.leader_uuid = p.uuid OR cm.player_uuid = p.uuid
                            ORDER BY (c.leader_uuid = p.uuid) DESC, c.name LIMIT 1
                        ) c ON TRUE
                        LEFT JOIN player_positions pos ON pos.player_uuid = p.uuid
                        LEFT JOIN LATERAL (SELECT COUNT(*) AS total FROM hardcore_bans WHERE player_uuid = p.uuid) hb ON TRUE
                        LEFT JOIN LATERAL (SELECT COUNT(*) AS total FROM tp_blocks WHERE blocker_uuid = p.uuid OR blocked_uuid = p.uuid) tb ON TRUE
                        LEFT JOIN LATERAL (SELECT COUNT(*) AS total FROM player_reports WHERE target_uuid = p.uuid) rt ON TRUE
                        LEFT JOIN LATERAL (SELECT COUNT(*) AS total FROM player_reports WHERE reporter_uuid = p.uuid) rs ON TRUE
                        ORDER BY lower(p.nickname)
                    """)
                    rows = cur.fetchall()
                    cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE' AND table_name = ANY(%s)", (list(PLUGIN_TABLES),))
                    table_count = len(cur.fetchall())
            self.tree.delete(*self.tree.get_children())
            for row in rows:
                self.tree.insert("", "end", values=tuple("" if value is None else str(value) for value in row))
            self.count_label.configure(text=f"Игроков: {len(rows)}")
            self.tables_label.configure(text=f"Таблицы плагина: {table_count}/{len(PLUGIN_TABLES)}")
            self.status.configure(text="Подключено · данные обновлены")
        except Exception as exc:
            self._show_error("Не удалось загрузить данные игроков", exc)
        finally:
            self.refresh_btn.configure(state="normal")

    def show_table_overview(self) -> None:
        try:
            with self._connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE' AND table_name = ANY(%s)", (list(PLUGIN_TABLES),))
                    present = {r[0] for r in cur.fetchall()}
                    counts: dict[str, int] = {}
                    for table in PLUGIN_TABLES:
                        if table in present:
                            cur.execute(f'SELECT COUNT(*) FROM "{table}"')
                            counts[table] = cur.fetchone()[0]
                rows = [(name, "BASE TABLE", counts[name]) for name in PLUGIN_TABLES if name in present]
            dialog = tk.Toplevel(self)
            dialog.title("Статистика таблиц PryanikPlugin")
            dialog.geometry("680x530")
            dialog.transient(self)
            ttk.Label(dialog, text="Все данные, которыми управляет плагин", font=("Segoe UI", 13, "bold")).pack(anchor="w", padx=14, pady=(14, 8))
            tree = ttk.Treeview(dialog, columns=("table", "rows", "status"), show="headings")
            for col, title, width in (("table", "Таблица", 270), ("rows", "Записей", 110), ("status", "Состояние", 180)):
                tree.heading(col, text=title)
                tree.column(col, width=width, anchor="w")
            tree.pack(fill="both", expand=True, padx=14, pady=(0, 12))
            counts = {r[0]: r[2] for r in rows}
            for name in PLUGIN_TABLES:
                exists = name in present
                count = counts.get(name)
                tree.insert("", "end", values=(name, "—" if count is None else count, "Есть" if exists else "Не создана"))
            ttk.Label(dialog, text=f"Создано таблиц: {len(present)} из {len(PLUGIN_TABLES)}. Отсутствующие будут пропущены при очистке.").pack(anchor="w", padx=14, pady=(0, 12))
        except Exception as exc:
            self._show_error("Не удалось прочитать таблицы", exc)

    def _homes_query(self):
        with self._connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    SELECT p.nickname, p.uuid::text, h.name, h.world_name,
                           h.x, h.y, h.z, h.yaw, h.pitch
                    FROM player_homes h
                    JOIN players p ON p.uuid = h.player_uuid
                    ORDER BY lower(p.nickname), lower(h.world_name), lower(h.name)
                """)
                return cur.fetchall()

    def show_homes(self) -> None:
        try:
            rows = self._homes_query()
        except Exception as exc:
            self._show_error("Не удалось загрузить личные дома", exc)
            return
        self._show_data_window(
            "Личные дома",
            ("Игрок", "UUID игрока", "Название", "Мир / гриф", "X", "Y", "Z", "Yaw", "Pitch"),
            rows,
            (140, 250, 150, 170, 90, 90, 90, 80, 80),
        )

    def _reports_query(self):
        with self._connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    SELECT r.reported_at, r.target_uuid::text, COALESCE(target.nickname, r.target_uuid::text),
                           r.reporter_uuid::text, COALESCE(reporter.nickname, r.reporter_uuid::text), r.reason
                    FROM player_reports r
                    LEFT JOIN players target ON target.uuid = r.target_uuid
                    LEFT JOIN players reporter ON reporter.uuid = r.reporter_uuid
                    ORDER BY r.reported_at DESC, r.id DESC
                """)
                return cur.fetchall()

    def show_reports(self) -> None:
        try:
            rows = self._reports_query()
        except Exception as exc:
            self._show_error("Не удалось загрузить жалобы", exc)
            return
        self._show_data_window("Жалобы игроков", ("Время", "На кого", "UUID нарушителя", "Кто пожаловался", "UUID жалобщика", "Причина"), rows, (155, 125, 250, 145, 250, 260))

    def _show_data_window(self, title: str, headers: tuple[str, ...], rows: list[tuple[Any, ...]], widths: tuple[int, ...]) -> None:
        dialog = tk.Toplevel(self)
        dialog.title(f"PryanikPlugin · {title}")
        dialog.geometry("1150x520")
        dialog.transient(self)
        cols = tuple(f"c{i}" for i in range(len(headers)))
        tree = ttk.Treeview(dialog, columns=cols, show="headings")
        for col, header, width in zip(cols, headers, widths):
            tree.heading(col, text=header)
            tree.column(col, width=width, minwidth=80, anchor="w", stretch=False)
        ybar = ttk.Scrollbar(dialog, orient="vertical", command=tree.yview)
        xbar = ttk.Scrollbar(dialog, orient="horizontal", command=tree.xview)
        tree.configure(yscrollcommand=ybar.set, xscrollcommand=xbar.set)
        tree.pack(fill="both", expand=True, padx=(12, 0), pady=(12, 0))
        ybar.pack(side="right", fill="y", pady=(12, 0))
        xbar.pack(fill="x", padx=12, pady=(0, 8))
        for row in rows:
            tree.insert("", "end", values=tuple("" if value is None else str(value) for value in row))
        ttk.Label(dialog, text=f"Записей: {len(rows)}").pack(anchor="w", padx=12, pady=(0, 10))

    def export_csv(self) -> None:
        from tkinter import filedialog
        path = filedialog.asksaveasfilename(title="Экспорт игроков", defaultextension=".csv", filetypes=[("CSV UTF-8", "*.csv")], initialfile="pryanik_players.csv")
        if not path:
            return
        try:
            import csv
            headers = [title for _, title, _ in PLAYER_COLUMNS]
            with open(path, "w", newline="", encoding="utf-8-sig") as stream:
                writer = csv.writer(stream, delimiter=";")
                writer.writerow(headers)
                for item in self.tree.get_children(""):
                    writer.writerow(self.tree.item(item, "values"))
            self.status.configure(text=f"CSV сохранён: {path}")
            messagebox.showinfo("Экспорт готов", f"Сохранено игроков: {len(self.tree.get_children())}\n{path}")
        except Exception as exc:
            messagebox.showerror("Ошибка экспорта", str(exc))

    def delete_selected(self) -> None:
        selected = self.tree.selection()
        if not selected:
            messagebox.showinfo("Не выбран игрок", "Выберите строку игрока в таблице.")
            return
        row = self.tree.item(selected[0], "values")
        nickname, player_uuid = row[0], row[1]
        if not messagebox.askyesno("Подтвердите удаление", f"Безвозвратно удалить аккаунт игрока «{nickname}» ({player_uuid}) и связанные данные?", icon="warning"):
            return
        try:
            with self._connection() as conn:
                with conn.cursor() as cur:
                    # Delete every direct reference before deleting the player row.
                    for table, column in (("player_reports", "target_uuid"), ("player_reports", "reporter_uuid"),
                                          ("player_activity", "player_uuid"), ("player_positions", "player_uuid"),
                                          ("hardcore_bans", "player_uuid"), ("clan_invites", "player_uuid"),
                                          ("clan_members", "player_uuid")):
                        cur.execute(f"DELETE FROM {table} WHERE {column}=%s", (player_uuid,))
                    cur.execute("DELETE FROM clans WHERE leader_uuid=%s", (player_uuid,))
                    cur.execute("DELETE FROM tp_blocks WHERE blocker_uuid=%s OR blocked_uuid=%s", (player_uuid, player_uuid))
                    cur.execute("DELETE FROM players WHERE uuid=%s RETURNING uuid", (player_uuid,))
                    deleted = cur.fetchone()
            if not deleted:
                messagebox.showwarning("Не найдено", "Аккаунт уже отсутствует в базе.")
            else:
                self.status.configure(text=f"Удалён игрок: {nickname}")
                self.load_players()
        except Exception as exc:
            self._show_error("Не удалось удалить игрока", exc)

    def confirm_wipe(self) -> None:
        dialog = tk.Toplevel(self)
        dialog.title("Полная очистка данных PryanikPlugin")
        dialog.transient(self)
        dialog.grab_set()
        dialog.resizable(False, False)
        text = ("Будут удалены все записи из таблиц данных PryanikPlugin:\n\n" + ", ".join(PLUGIN_TABLES) +
                "\n\nТаблицы будут найдены только в текущей схеме PostgreSQL.\n"
                "Сами таблицы, индексы и миграционная история останутся.\n"
                "Посторонние таблицы и сама база данных не затрагиваются.\n\n"
                "Рекомендуется остановить Paper перед очисткой.")
        ttk.Label(dialog, text=text, justify="left", wraplength=580).pack(padx=18, pady=(16, 12), anchor="w")
        ttk.Label(dialog, text="Пароль БД из главного окна:").pack(padx=18, anchor="w")
        password = ttk.Entry(dialog, width=52, show="*")
        password.pack(padx=18, pady=(4, 10), fill="x")
        ttk.Label(dialog, text="Для подтверждения введите точно: DELETE PRYANIK DATA").pack(padx=18, anchor="w")
        confirm = ttk.Entry(dialog, width=52)
        confirm.pack(padx=18, pady=(4, 12), fill="x")
        buttons = ttk.Frame(dialog)
        buttons.pack(fill="x", padx=18, pady=(0, 16))
        ttk.Button(buttons, text="Отмена", command=dialog.destroy).pack(side="right")
        ttk.Button(buttons, text="Очистить таблицы", style="Danger.TButton", command=lambda: self._wipe_all(dialog, password, confirm)).pack(side="right", padx=(0, 8))
        password.focus_set()

    def _wipe_all(self, dialog: tk.Toplevel, password_entry: ttk.Entry, confirmation: ttk.Entry) -> None:
        if not self.fields["password"].get() or password_entry.get() != self.fields["password"].get():
            messagebox.showerror("Пароль не совпадает", "Введите тот же пароль БД, что указан в главном окне.", parent=dialog)
            return
        if confirmation.get() != "DELETE PRYANIK DATA":
            messagebox.showerror("Неверное подтверждение", "Введите DELETE PRYANIK DATA точно.", parent=dialog)
            return
        if not messagebox.askyesno("Последнее подтверждение", "Очистить данные всех найденных таблиц PryanikPlugin? Это действие нельзя отменить.", icon="error", parent=dialog):
            return
        counts: dict[str, int] = {}
        try:
            # psycopg's connection context wraps all deletes in one transaction.
            with self._connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE' AND table_name = ANY(%s)", (list(PLUGIN_TABLES),))
                    present = {row[0] for row in cur.fetchall()}
                    for table in PLUGIN_TABLES:
                        if table not in present:
                            counts[table] = 0
                            continue
                        cur.execute(f'DELETE FROM "{table}"')
                        counts[table] = max(cur.rowcount, 0)
            dialog.destroy()
            self.tree.delete(*self.tree.get_children())
            self.count_label.configure(text="Игроков: 0")
            total = sum(counts.values())
            self.status.configure(text=f"Очистка завершена · записей удалено: {total}")
            self.tables_label.configure(text=f"Таблицы проверены: {len(counts)}")
            details = "\n".join(f"{table}: {count}" for table, count in counts.items())
            messagebox.showinfo("Очистка завершена", f"Удалено записей: {total}\n\n{details}\n\nТаблицы и сама база данных сохранены.")
        except Exception as exc:
            messagebox.showerror("Очистка отменена", f"Не удалось очистить таблицы. Транзакция откатила все изменения, частичная очистка не сохранена.\n\n{exc}", parent=dialog)


def main() -> None:
    app = PlayersViewer()
    app.mainloop()


if __name__ == "__main__":
    main()
