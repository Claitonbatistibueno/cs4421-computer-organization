# CS4421 Hardware Inspector

A Java program that reads information about the computer it runs on and prints it as plain text: processor, memory, disk, PCI devices and USB devices. It also runs a parallelism benchmark and gives a plain-English verdict on what the computer is good for.

All data comes from the text files Linux provides under `/proc` and `/sys`. The program runs from a terminal with no graphical display, so it works on the alderaan cloud server.

# Build and run

Requirements: Linux and Java 17 or newer. WSL2 runs a real Linux kernel, so the program works there, but it only sees the hardware of the WSL2 virtual machine (usually no PCI or USB devices). Windows and macOS do not have `/proc` and `/sys`, so the program cannot read the hardware there.

Run every command from the `hw-starter` folder, so the program can find the `ids` folder:

```
cd hw-starter
javac -d out src/*.java
java -cp out display
```

`display` opens a menu:

1. System Stats: processor details, disk, memory, CPU load, CPU benchmark score, PCI devices and USB devices
2. Parallelism & Verdict: the parallelism benchmark, followed by the verdict (extension)
3. Export HTML: saves the report as `report.html` in the `hw-starter` folder (extension)
4. Exit

To print everything at once, without the menu:

```
java -cp out display all
```

Option 1 takes about 5 seconds: 1 second to measure the CPU load and about 4 seconds for the CPU benchmark (only the first time it is chosen). Option 2 takes about 10 to 15 seconds, because each test runs 3 times.

## Where each piece of information comes from

| Information | Source file | Class |
| --- | --- | --- |
| Number of cores | `/proc/cpuinfo` (counts the `processor` lines) | `cpuInfo` |
| Processor model | `/proc/cpuinfo` (`model name` line). On arm64 there is no such line, so the `CPU implementer` and `CPU part` numbers are shown instead | `cpuInfo` |
| Frequency, logical threads, physical cores | `/proc/cpuinfo` (`cpu MHz`, `siblings`, `cpu cores` lines; shown as unknown when missing, as on arm64) | `cpuInfo` |
| Running in a virtual machine | `/proc/cpuinfo` (`flags` line contains `hypervisor`) | `cpuInfo` |
| CPU load per core and average | `/proc/stat` (`cpu0`, `cpu1`, ... lines). The counters are time since boot, so we read them twice, 1 second apart, and use the difference: load = Δbusy ÷ (Δbusy + Δidle) | `cpuInfo` |
| Total and used memory | `/proc/meminfo` (`MemTotal`, `MemAvailable`) | `memInfo` |
| Disk type | `/sys/block/<disk>/queue/rotational` of the first real disk (loop, ram, CD, floppy and similar devices are skipped): 0 = SSD, 1 = HDD | `diskInfo` |
| PCI devices (bus, device, function, vendor ID, device ID, class) | `/sys/bus/pci/devices/<address>/` (`vendor`, `device`, `class` files) | `pciInfo` |
| USB devices (vendor ID, product ID, bus, device number) | `/sys/bus/usb/devices/<device>/` (`idVendor`, `idProduct`, `busnum`, `devnum` files) | `usbInfo` |
| Vendor, device and class names | `ids/pci.ids` and `ids/usb.ids` (if a name is not found, the raw ID is shown) | `idLookup` |
| Graphics card | PCI devices with base class `03` (display controller) | `pciInfo` |

# What the program reports

1. Processor: number of cores, model, frequency, threads, physical cores, whether it runs in a virtual machine, load per core and average load.
2. Memory: total, used and percentage in use (in GiB).
3. PCI devices: every device with its address (domain:bus:device.function), vendor and device IDs and names, and its class, stored in an array. If there are none (common on WSL2), the program says so.
4. USB devices: every device with its vendor and product IDs, stored in an array. If there are none (common on virtual machines and the cloud server), the program says so.
5. Names: IDs are looked up in `pci.ids` and `usb.ids`. Unknown devices show their raw ID.
6. Analysis: the PCI list is sorted by vendor name and by bus/device/function, with a count of devices per vendor and per class (`displayUniqueVendors`, `displayUniqueClasses`). Both counts appear in the text report and in the HTML report.

# Extension: parallelism and Amdahl's law

The program counts the prime numbers below 5,000,000 using 1, 2, 4 and 8 threads, and measures how much faster it gets.

- **Speedup** = time with 1 thread ÷ time with N threads.
- **Efficiency** = speedup ÷ N.
- From the measured speedup we estimate the parallel fraction *p* of the task, and compare our results with the limit predicted by **Amdahl's law**: S(N) = 1 ÷ ((1 − p) + p ÷ N).
- While each test runs, the program shows the **load of every core**, using the same `/proc/stat` method as the main report.

The work is split into blocks of 10,000 numbers handed out in turn to each thread. We first tried giving each thread every N-th number, but then one thread got all the even numbers (which are rejected straight away) and the speedup was almost zero. Blocks keep the work balanced.

With more threads than cores, the extra threads only share the same cores, so the speedup stops growing at the number of cores.

# Verdict: "This computer is good for..."

Using the number of cores, total RAM, disk type, whether there is a graphics card, and the measured speedup, the program rates the computer for Study, Programming, Video editing and Gaming as **YES**, **PARTLY** or **NO**, and prints the reason for each threshold.

In a virtual machine (such as alderaan or WSL2) the disk is a file on the host, so the SSD or HDD value is whatever the virtual machine reports, not the real drive. The program prints a note when this happens.

# CPU benchmark score

Option 1 also shows a single-core CPU score (`cpuBench`). It fills 16 MiB with random data, then measures how fast it can hash it with SHA-256 and compress and decompress it with zip. After 2 warm-up runs it takes the average of 5 runs, in MiB per second: this is the raw score. The CPU score is the raw score divided by a reference value of 578.28, so 1.00 means the same speed as the reference.

# Extension: HTML report

The same report can be saved as a web page, to open on your own computer:

```
cd hw-starter
javac -d out src/*.java
java -cp out display --html report.html
```

This creates `report.html` with the processor, memory, PCI and USB sections as tables. The page uses the same data as the text report, has its CSS inside the file and needs no internet. Without `--html` the program works as before, in text only.
