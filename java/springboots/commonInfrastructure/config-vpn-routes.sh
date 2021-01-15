#!/usr/bin/env bash
#
# This script is used to reconfigure OSX iptables upon connecting to a VPN,
# such that the VPN's tunX interface will not be registered as a "default" route,
# and only specific routes will be sent through the tunnel

if (( EUID != 0 )); then
  echo "Please, run this command with sudo" 1>&2
  exit 1
fi

# configure values we expect for local (ethernet/wifi) and vpn (tunnel) interfaces
LOCAL_NET_INTERFACE=en0
TUNNEL_INTERFACE=utun1
# derive default gateway from local network
GATEWAY=$(netstat -nrf inet | grep default | grep $LOCAL_NET_INTERFACE | awk '{print $2}')

echo "Resetting routes with gateway => $GATEWAY"
echo
# remove default routes
route -n delete default -ifscope $LOCAL_NET_INTERFACE
route -n delete -net default -interface $TUNNEL_INTERFACE
# add back default route for locate gateway
route -n add -net default $GATEWAY
# add routes for each subnet we wish to send through vpn
for subnet in  10 172.16
do
  route -n add -net $subnet -interface $TUNNEL_INTERFACE
done
