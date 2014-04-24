#!/bin/bash

while true; do impala-shell -q "invalidate metadata;"; sleep 5; done