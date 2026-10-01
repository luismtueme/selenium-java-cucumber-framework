// Fake clock installed by BrowserClock as a WebDriver BiDi preload script: it runs in every new
// document before the page's own scripts. Time stands still at the moment the page loaded until
// window.__testClock.tick(ms) moves it forward, firing due timers in order.
() => {
    if (window.__testClock) return;

    const RealDate = Date;
    let now = RealDate.now();
    let nextId = 1;
    const timers = new Map();

    class FakeDate extends RealDate {
        constructor(...args) {
            if (args.length === 0) super(now);
            else super(...args);
        }
        static now() {
            return now;
        }
    }
    window.Date = FakeDate;

    const schedule = (callback, delay, args, repeat) => {
        const id = nextId++;
        const ms = Math.max(0, Number(delay) || 0);
        timers.set(id, { at: now + ms, callback, args, interval: repeat ? Math.max(1, ms) : null });
        return id;
    };
    window.setTimeout = (callback, delay, ...args) => schedule(callback, delay, args, false);
    window.setInterval = (callback, delay, ...args) => schedule(callback, delay, args, true);
    window.clearTimeout = (id) => void timers.delete(id);
    window.clearInterval = window.clearTimeout;

    window.__testClock = {
        tick(ms) {
            const target = now + ms;
            for (;;) {
                let due = null;
                for (const [id, timer] of timers) {
                    if (timer.at <= target && (!due || timer.at < due.timer.at)) due = { id, timer };
                }
                if (!due) break;
                now = due.timer.at;
                if (due.timer.interval === null) timers.delete(due.id);
                else due.timer.at += due.timer.interval;
                if (typeof due.timer.callback === 'function') due.timer.callback(...due.timer.args);
            }
            now = target;
        },
    };
}
