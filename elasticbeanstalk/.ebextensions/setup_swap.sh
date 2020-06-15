#!/bin/bash

SWAPFILE=/var/swapfile
# Create a swapfile twice the physical size
SWAP_MEGABYTES=$(free -m | awk '$0 ~ /Mem/ {print $2 * 2;}')

if [ -f $SWAPFILE ]; then
  echo "Swapfile $SWAPFILE found, assuming already setup"
  exit;
fi

/bin/dd if=/dev/zero of=$SWAPFILE bs=1M count=$SWAP_MEGABYTES
/bin/chmod 600 $SWAPFILE
/sbin/mkswap $SWAPFILE
/sbin/swapon $SWAPFILE